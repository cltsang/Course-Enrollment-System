DROP TABLE enroll CASCADE CONSTRAINTS;
DROP TABLE lecture CASCADE CONSTRAINTS;
DROP TABLE section CASCADE CONSTRAINTS;
DROP TABLE prerequisite CASCADE CONSTRAINTS;
DROP TABLE course CASCADE CONSTRAINTS;
DROP TABLE student CASCADE CONSTRAINTS;


CREATE TABLE student(
	sid NUMBER(10) PRIMARY KEY,
	name CHAR(25) NOT NULL,
	major CHAR(100) NOT NULL
);

CREATE TABLE course(
	code CHAR(8) PRIMARY KEY,
	name CHAR(100) NOT NULL,
	credit NUMBER(1) NOT NULL,
	CHECK(credit >= 1 AND credit <= 3)
);

CREATE TABLE prerequisite(
	course CHAR(8),
	precourse CHAR(8),
	PRIMARY KEY(course, precourse),
	FOREIGN KEY(course) REFERENCES course(code),
	FOREIGN KEY(precourse) REFERENCES course(code)
);

CREATE TABLE section(
	code CHAR(8),
	year NUMBER(4),
	term NUMBER(1),
	quota NUMBER(2) NOT NULL,
	instructor CHAR(25) NOT NULL,
	PRIMARY KEY(code, year, term),
	FOREIGN KEY(code) REFERENCES course,
	CHECK(year > 0),
	CHECK(term >= 1 AND term <= 3),
	CHECK(quota > 0)
);

CREATE TABLE lecture(
	code CHAR(8),
	year NUMBER(4),
	term NUMBER(1),
	timeslot CHAR(3),
	PRIMARY KEY(code, year, term, timeslot),
	FOREIGN KEY(code, year, term) REFERENCES section(code, year, term)
);

CREATE TABLE enroll(
	sid NUMBER(10),
	code CHAR(8),
	year NUMBER(4),
	term NUMBER(1),
	grade CHAR(1),
	PRIMARY KEY(sid, code, year, term),
	FOREIGN KEY(sid) REFERENCES student(sid),
	FOREIGN KEY(code, year, term) REFERENCES section(code, year, term)
);
