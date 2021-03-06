/*
 * This file is part of gwap, an open platform for games with a purpose
 *
 * Copyright (C) 2013
 * Project play4science
 * Lehr- und Forschungseinheit für Programmier- und Modellierungssprachen
 * Ludwig-Maximilians-Universität München
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package gwap.game;

import gwap.model.GameRound;
import gwap.model.GameSession;
import gwap.model.GameType;
import gwap.model.Person;
import gwap.model.action.Action;
import gwap.model.resource.IpBasedLocation;
import gwap.tools.IpBasedLocationBean;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.log.Log;

import com.maxmind.geoip.Location;

/**
 * This is the backing bean for one game session. It handles all actions that
 * can be executed during a game session. The game session itself is organized
 * in a business process.
 * 
 * @author Christoph Wieser
 */
public abstract class AbstractGameSessionBean implements Serializable {

	private static final long serialVersionUID = 1L;

	@Create                  public void init() { log.info("Creating"); }
	@Destroy                 public void destroy() { log.info("Destroying"); }
	
	@Logger                  protected Log log;
	@In                      protected FacesMessages facesMessages;
	@In                      protected IpBasedLocationBean ipBasedLocationBean;
	@In                      protected EntityManager entityManager;
	@In(create=true)	     protected Person person;
	@In(create=true) @Out	 protected GameSession gameSession;
	@In(required=false) @Out protected GameRound gameRound;
	@In(required=false) @Out protected GameType gameType;
	
	protected Integer completedRoundsScore;
	protected Integer currentRoundScore;
	
	protected Integer roundsLeft;
	protected Integer roundNr = 1;
	protected int maxClientDelay = 3000; // milliseconds
	protected Date startConsideringClientDelay;
	@In("#{facesContext.externalContext.request.remoteAddr}") private String remoteAddr;
	
	public void startGameSession() {	
		startGameSession("imageLabeler");
	}

	public void startGameSession(String gameName) {
		log.info("Starting game session");
		Query query = entityManager.createNamedQuery("gameType.select");
		query.setParameter("name", gameName);
		gameType = (GameType) query.getSingleResult();
		gameSession.setGameType(gameType);
		entityManager.persist(gameSession);
		roundsLeft = gameType.getRounds();
		completedRoundsScore = 0;
		lookupIpBasedLocation();
		startRound();
	}

	public void endGameSession() {
		log.info("Ending game session");
		
		entityManager.merge(gameSession);
	}

	public boolean startRound() {
		if (gameRound != null && gameRound.getNumber() != null && gameRound.getNumber().equals(roundNr)) {
			log.info("Omit starting game round #0 again, because it is already started", roundNr);
			return false;
		}
		log.info("Starting game round #0 (#1 left)", roundNr, roundsLeft);
		currentRoundScore = 0;
		gameRound = new GameRound();
		gameRound.setStartDate(new Date());
		startConsideringClientDelay = null;
		gameRound.setPerson(person);
		gameRound.setNumber(roundNr);
		gameRound.setGameSession(gameSession);
		gameSession.getGameRounds().add(gameRound);
		entityManager.persist(gameRound);
		loadNewResource();
		return true;
	}

	/**
	 * Needs to load a new resource before each round. It is called
	 * in the startRound() method.
	 */
	protected abstract void loadNewResource();
	
	public void endRound() {
		log.info("Ending round");
		if (roundsLeft != null)
			roundsLeft--;
		roundNr++;
		gameRound.setEndDate(new Date());
		gameRound.setScore(currentRoundScore);
		if (currentRoundScore != null)
			completedRoundsScore += currentRoundScore;
		currentRoundScore = null;
		entityManager.merge(gameRound);
		entityManager.flush();
	}
	
	public boolean roundExpired() {
		Date now = new Date();
		Date roundStart = gameRound.getStartDate();
		Integer roundDuration = gameType.getRoundDuration();
		
		// Calculate round expiration for the first call of this method by the client
		if (startConsideringClientDelay == null) {
			Date clientStartDate = new Date();
		
			// clientDelay: delay between roundStart(server) and rendering complete(client)  
			Calendar roundStartCalendar = new GregorianCalendar();
			Calendar clientStartCalendar = new GregorianCalendar();
			roundStartCalendar.setTime(roundStart);
			clientStartCalendar.setTime(clientStartDate);
			Integer clientDelay = (int) (clientStartCalendar.getTimeInMillis()-roundStartCalendar.getTimeInMillis());
			
			// latest start of round considering client delay
			Calendar maxStartConsideringClientDelayCalendar = new GregorianCalendar();
			maxStartConsideringClientDelayCalendar.setTime(roundStart);
			maxStartConsideringClientDelayCalendar.add(Calendar.MILLISECOND, maxClientDelay);
			Date maxStartConsideringClientDelay = maxStartConsideringClientDelayCalendar.getTime();
	
			// limiting client delay
			startConsideringClientDelay = (clientDelay > maxClientDelay) ? maxStartConsideringClientDelay : clientStartDate;
		}
	
		Calendar calendar = new GregorianCalendar();
		calendar.setTime(startConsideringClientDelay);
		calendar.add(Calendar.SECOND, roundDuration);
		Date expectedExpiration = calendar.getTime();
		
		return expectedExpiration.before(now);
	}
	
	public Integer getRoundsLeft() {
		return roundsLeft;
	}
	
	@Out("gameSessionScore")
	public Integer getScore() {
		if (currentRoundScore == null)
			return completedRoundsScore;
		else if (completedRoundsScore == null)
			return currentRoundScore;
		else
			return completedRoundsScore + currentRoundScore;
	}
	
	public GameType getGameType() {
		return gameType;
	}

	/**
	 * Assigns created, person and gameround to the current values
	 */
	public void initializeAction(Action action) {
		action.setCreated(new Date());
		action.setPerson(person);
		action.setGameRound(gameRound);
	}
	
	/**
	 * Performs a lookup of the location of the current user and saves it 
	 * to the current gameSession;
	 */
	public void lookupIpBasedLocation() {
    	try {
    		IpBasedLocation ipBasedLocation = null;
			Location l = ipBasedLocationBean.findByIpAddress(remoteAddr);
			if (l != null) {
				String regionName = com.maxmind.geoip.regionName.regionNameByCode(l.countryCode, l.region);
				Query q;
				if (regionName == null && l.city == null) {
					q = entityManager.createNamedQuery("byCountryWithoutRegionAndCity");
				} else {
					q = entityManager.createNamedQuery("byCountryRegionCity");
					q.setParameter("region", regionName);
					q.setParameter("city", l.city);
				}
				q.setParameter("country", l.countryName);
				q.setMaxResults(1);
				try {
					ipBasedLocation = (IpBasedLocation) q.getSingleResult();
				} catch (NoResultException e) {
					ipBasedLocation = new IpBasedLocation();
				    ipBasedLocation.setCity(l.city);
				    ipBasedLocation.setRegion(regionName);
				    ipBasedLocation.setCountry(l.countryName);
				    entityManager.persist(ipBasedLocation);
				}
				log.info("IpBasedLocation set to #0", ipBasedLocation);
				gameSession.setIpBasedLocation(ipBasedLocation);
			}
		}
		catch (Throwable e) {
		   log.info(e.toString());
		}
    }
}
