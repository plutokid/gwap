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

package gwap.game.test;

import gwap.tools.Pair;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.annotations.Scope;

@Scope(ScopeType.APPLICATION)
@Name("gwapGameTestPlayerMatcher")
public class PlayerMatcher extends gwap.game.PlayerMatcher<SharedGame, Player> {

	private static final long serialVersionUID = 1L;

	@Out(required = true)
	private SharedGame gwapGameTestSharedGame;
	
	@Out(required = true)
	private Player gwapGameTestPlayer;

	public PlayerMatcher() {
		super("gwapGameTest");
	}
	
	public void enqueue() {
		Pair<SharedGame, Player> p = match();
		gwapGameTestSharedGame = p.a;
		gwapGameTestPlayer = p.b;
	}
}
