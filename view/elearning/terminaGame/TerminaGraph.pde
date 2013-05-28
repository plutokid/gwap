Vertex centerVertex;
float cx;
float cy;
String term;
ArrayList<Vertex> vertices;
ArrayList<Vertex> ownTags;
ArrayList<Vertex> foreignTags;
ArrayList<Vertex> correctTags;
ArrayList<Vertex> unknownTags;
ArrayList<Vertex> wrongTags;

String newTerm;
boolean doneMoving;

boolean high;
RoundedArc arcDeTriomphe; 

color BACKGROUND = color(255);  
color STROKE = color(0);
color BLUE = color(14, 30, 191);
color YELLOW = color(255, 216, 59);
color RED = color(255, 55, 55);
color TEXT = color(0);
color GREY = color(220);

color CORRECT = #00FF00;
color WRONG = #FF0000;
color UNKNOWN = #CC8811;

void setup() {
	this.width = 400;
	this.height = 300;
	size(width, height);
	cx = width/2;
	cy = height/2;

	high = false;
	arcDeTriomphe = new RoundedArc(0, 1, 100, 20);

	background(BACKGROUND);
	term = "term";
	newTerm = "";
	centerVertex = new Vertex(cx, cy, 20, term, 0, GREY);
	centerVertex.display();
	vertices = new ArrayList(0);
	ownTags = new ArrayList(0);
	foreignTags = new ArrayList(0);
	correctTags = new ArrayList(0);
	wrongTags = new ArrayList(0);
	unknownTags = new ArrayList(0);

	stroke(STROKE);
	fill(YELLOW);

	frameRate(15);
}

void draw() {
	background(BACKGROUND);
	collide();

	if(correctTags.size() > 0 && unknownTags.size() > 0 && wrongTags.size() > 0){
//		updateVertexDistances();
	}

	for(Vertex v : vertices)
		v.move();

	if (high && ownTags.size() > 0){
		updateArc();
		arcDeTriomphe.display();    
	}

	fill(STROKE);
	stroke(STROKE);

	for (Vertex v : vertices) {

		line(cx, cy, v.x, v.y);
		v.display();
	}
	centerVertex.display();
}

void setTerm(String term) {
	this.term = term;    
	centerVertex = new Vertex(cx, cy, 20, term, 0, GREY);
}

float getBiggestCornerDistance(ArrayList<Vertex> al){
	float outerRadius = 0;

	for(Vertex v : al){
		d_l_up = dist(v.x - v.tw /2, v.y - v.th / 2, cx ,cy);
		d_l_down = dist(v.x - v.tw /2, v.y + v.th / 2, cx ,cy);
		d_r_up = dist(v.x + v.tw /2, v.y - v.th / 2, cx ,cy);
		d_r_down = dist(v.x + v.tw /2, v.y + v.th / 2, cx ,cy);
		float[] arr = {d_l_up, d_l_down, d_r_up, d_r_down};
		m = max(arr);
		if (m > outerRadius)
			outerRadius = m;
	}
	return outerRadius;
}

void updateArc(){
	outerRadius = getBiggestCornerDistance(ownTags);
	arcDeTriomphe.rs = outerRadius - arcDeTriomphe.r ;

	float start = - ownTags.get(0).angle + HALF_PI;
	float stop = - ownTags.get(ownTags.size() - 1).angle + HALF_PI;

	if(start < stop){
		arcDeTriomphe.start = start;
		arcDeTriomphe.stop =  stop;
	} else {
		arcDeTriomphe.start = stop;
		arcDeTriomphe.stop =  start;       
	}
	noStroke();
	float pStart_x = cx + arcDeTriomphe.r * cos(arcDeTriomphe.start);
	float pStart_y = cy + arcDeTriomphe.r * sin(arcDeTriomphe.start);

	float pStop_x = cx + arcDeTriomphe.r * cos(arcDeTriomphe.stop);
	float pStop_y = cy + arcDeTriomphe.r * sin(arcDeTriomphe.stop);

	Vertex v = ownTags.get(0);

	float p1x = v.x + v.tw / 2;
	float p1y = v.y + v.th / 2;
	float p2x = v.x - v.tw / 2;
	float p2y = v.y + v.th / 2;
//	println(p1x + " " + p1y + " " + pStart_x + " " +pStop_x  + " " + arcDeTriomphe.rs +  " " +  dist(p1x,p1y, pStart_x, pStart_y) );
	if(dist(p1x,p1y, pStart_x, pStart_y) > arcDeTriomphe.rs){
		float alpha = acos((p1x - cx)/arcDeTriomphe.r);
		float beta = 2 * asin(arcDeTriomphe.rs / (2* arcDeTriomphe.r));
//		println("debording " + v.s);
		arcDeTriomphe.stop = - alpha - beta;
	} else if(dist(p2x,p2y,pStop_x, pStop_y) > arcDeTriomphe.rs){
		float alpha = acos((p2x - cx)/arcDeTriomphe.r);
		float beta = 2 * asin(arcDeTriomphe.rs / (2* arcDeTriomphe.r));
		println("debording " + v.s);
		arcDeTriomphe.start = - alpha + beta;

	}
	arcDeTriomphe.rs *= 1.1;

}

void collide() {
	if (vertices.size() > 1) {
		for (int i = 0; i < vertices.size(); i++) {
			Vertex v = vertices.get(i);
			Vertex w = vertices.get((i + 1) % vertices.size());
			int w1 = (int)( v.tw / 2);
			int w2 = (int)( w.tw / 2);
			int h1 = (int)( v.th / 2);
			int h2 = (int)( w.th / 2);
			int dx = abs(v.x - w.x);
			int dy = abs(v.y - w.y);
			if ( (w1 + w2) > dx && (h1 + h2) > dy) {
				if (i == 0) {
					w.newAngle += 0.02;
				} 
				else if (i == vertices.size() - 1) {
					v.newAngle -= 0.02;
				} 
				else {
					v.newAngle -= 0.02;
					w.newAngle += 0.02;
				}
			}
		}
	}
}


void mix(){
	for(int i = 0; i < vertices.size(); i++){
		int j = (int)random(0, vertices.size());
		Vertex v = vertices.get(i);
		Vertex w = vertices.get(j);
		vertices.set(j,v);
		vertices.set(i,w);
		v.setMovement(j,vertices.size());
		w.setMovement(i,vertices.size());
	}  
}

Vertex newVertex(String s, float distance, int size, String matchType) {
	Vertex vert = new Vertex(cx, cy - distance, size, s, distance, new color(0));

	if(matchType == "directMatch"){
		vert.c = CORRECT;
		correctTags.add(vert);
		println("new correct tag " + correctTags.size() );
	} else if(matchType == "indirectMatch"){
		vert.c = UNKNOWN;
		unknownTags.add(vert);
		println("new unknown tag " + unknownTags.size());
	} else {
		vert.c = WRONG;
		wrongTags.add(vert);
		println("new wrong tag " + wrongTags.size());
	}
	return vert;
}

void updatePositions(){
	int n = vertices.size();
	if (n < 7) { //small amount
		for (int i = 1; i < n; i++) {

			int to = i;
			int off = 7;

			Vertex v = vertices.get(i);
			v.setMovement(to, off);
		}
	} 
	else {
		for (int i = 1; i < n; i++) {
			int to = i;
			int off = n + 1;
			Vertex v = vertices.get(i);
			v.setMovement(to, off);
		}
	}
}

void setDisplayWidth(int newWidth){
	this.width = newWidth;
//	externals.canvas.width = width;
	size(newWidth, height);
	cx = (int) newWidth / 2;
	centerVertex.x = cx;
	for(Vertex v : vertices)
		v.move();

	draw();
}

void addOwnTag(String s, int distance, int size, String matchType){
	Vertex vert = newVertex(s, distance, size, matchType);
	vertices.add(0, vert);
	updatePositions();
	ownTags.add(0, vert);
}

void separateTags(){
	_vertices = new ArrayList();
	for(Vertex v : ownTags)
		_vertices.add(v);

	for(Vertex v : foreignTags)
		_vertices.add(v);

	vertices = _vertices; 

	int n = vertices.size();
	int owns = ownTags.size();
	int foreigns = 0;
	for(int i = 0; i < n; i++){
		Vertex vert = vertices.get(i);

		if (i < owns){
			vert.setMovement(i, n + 2);
		} else {
			vert.setMovement(i + 1, n + 2);
		}

	}  

}

void addForeignTag(String s, int distance, int size, String matchType){
	Vertex vert = newVertex(s, distance, size, matchType);

	vertices.add(0,vert);
	updatePositions();

	foreignTags.add(0, vert);
}

String getOwnTags(){
	String s = "";
	for (Vertex vert : ownTags){
		s = s + " " + vert.s;
	}
	return s;
}

String getForeignTags(){
	String s = "";
	for (Vertex vert : foreignTags){
		s = s + " " + vert.s;
	}
	return s;
} 

void highlightOwnTags(){
	high = true; 
}

void downlightOwnTags(){
	high = false;   
}

void updateVertexDistances(){
	
	println("scaling distances!");
	float currdist = 100; 
	
	for(Vertex v : correctTags){
		v.distance = currdist;
		println("setting all correct tags to distance " + currdist)
	}
	
	currdist = getBiggestCornerDistance(correctTags);

	for(Vertex v : unknownTags){
		v.distance = currdist;
		println("setting unknown tags to distance " + currdist);
	}

	currdist2 = getBiggestCornerDistance(unknownTags);
	if(currdist2 > currdist)
		currdist = currdist2;

	for(Vertex v : wrongTags){
		v.distance = currdist;
		println("setting all wrong tags to distance " + currdist);
	}
}
