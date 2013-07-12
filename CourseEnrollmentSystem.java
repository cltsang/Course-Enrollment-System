import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.*;

public class CourseEnrollmentSystem {
	final String url = "jdbc:oracle:thin:@db12.cse.cuhk.edu.hk:1521:db12";
	final String id = "a011";//"a008";// "a010";
	final String pw = "krxuypzx";//"tsqdoafg";// "";

	final String studentDataPath = "dataset/student.txt";
	final String courseDataPath = "dataset/course.txt";
	final String sectionDataPath = "dataset/section.txt";
	final String enrollDataPath = "dataset/enrollment.txt";

	Connection connection;
	private String user;

	public CourseEnrollmentSystem() {
		connectDatabase(id, pw);
	}

	@Override
	public void finalize() {
		disconnectDatabase();
	}

	public void run() {
		if (user == null)
			return;

		Scanner input;
		String command;

		while (true) {
			System.out.println("========== ========== ========== ========== ========== ==========");
			System.out.print("Please enter a command: ");
			input = new Scanner(System.in);
			try {
				command = input.nextLine();
			} catch (NoSuchElementException e) {
				break;
			}

			if (command.equals("quit")) {
				break;
			}

			String[] tokens = command.split(" ");
			if (user.equals("admin")) {
				if (tokens[0].equals("create"))
					createTables();
				else if (tokens[0].equals("drop"))
					dropTables();
				else if (tokens[0].equals("insert"))
					insertDataFromFile();
				else if (tokens[0].equals("show"))
					showInformation();
				else if (tokens[0].equals("rank")) {
					if (tokens.length != 3){
						System.out.println("Please input: rank [year] [term]");
						continue;
					}

					try{
						Integer.parseInt(tokens[1]);
					} catch (NumberFormatException e){
						System.out.println("year entered is not an integer.");
						continue;
					}
					if(!tokens[2].equals("1") && !tokens[2].equals("2") && !tokens[2].equals("3")){
						System.out.println("Term must be 1 or 2 or 3.");
						continue;
					}

					showEnrollmentRatings(tokens[1], tokens[2]);
				}
				else
					System.out.println(command + " is not a valid command.");
			}
			else { // a student
				if(tokens[0].equals("enroll")){
					if(tokens.length != 4){
						System.out.println("Please input: enroll [code] [year] [term]");
						continue;
					}

					try{
						Integer.parseInt(tokens[2]);
					} catch (NumberFormatException e){
						System.out.println("year entered is not an integer.");
						continue;
					}
					if(!tokens[3].equals("1") && !tokens[3].equals("2") && !tokens[3].equals("3")){
						System.out.println("Term must be 1 or 2 or 3.");
						continue;
					}

					enrollCourse(tokens[1], tokens[2], tokens[3]);
				}
				else if(tokens[0].equals("drop")){
					if(tokens.length != 4){
						System.out.println("Please input: drop [code] [year] [term]");
						continue;
					}

					try{
						Integer.parseInt(tokens[2]);
					} catch (NumberFormatException e){
						System.out.println("year entered is not an integer.");
						continue;
					}
					if(!tokens[3].equals("1") && !tokens[2].equals("2") && !tokens[2].equals("3")){
						System.out.println("Term must be 1 or 2 or 3.");
						continue;
					}

					dropCourse(tokens[1], tokens[2], tokens[3]);
				}
				else if (tokens[0].equals("report")){
					if(tokens.length != 3){
						System.out.println("Please input: rank [year] [term]");
						continue;
					}

					try{
						Integer.parseInt(tokens[1]);
					} catch (NumberFormatException e){
						System.out.println("year entered is not an integer.");
						continue;
					}
					if(!tokens[2].equals("1") && !tokens[2].equals("2") && !tokens[2].equals("3")){
						System.out.println("Term must be 1 or 2 or 3.");
						continue;
					}

					showReport(tokens[1], tokens[2]);
				}
				else
					System.out.println(command + " is not a valid command.");
			}
		}
		System.out.println("========== ========== ========== ========== ========== ==========");

		//disconnectDatabase();
	}

	private boolean prerequisiteMet(String code, String year, String term){
		boolean returnValue = true;
		try{
			Statement statement = connection.createStatement();
			String query = "SELECT precourse FROM prerequisite WHERE course='" + code + "' "
					+ "MINUS "
					+ "SELECT code FROM enroll WHERE sid=" + user + " AND grade<>'F' AND "
					+ "(year<" + year + " or (year=" + year + " and term<" + term + "))";
			statement.executeQuery(query);
			ResultSet results = statement.getResultSet();
			while(results.next()){
				System.out.println("You have not taken " + results.getString(1));
				returnValue = false;
			}

			statement.close();
			return returnValue;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	private boolean courseExists(String code){
		try{
			Statement statement = connection.createStatement();
			statement.executeQuery("SELECT * FROM section WHERE code='" + code + "'");
			ResultSet result = statement.getResultSet();
			if(result.next()){
				statement.close();
				return true;
			}
			else{
				statement.close();
				return false;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	private boolean sectionExists(String code, String year, String term){
		try{
			Statement statement = connection.createStatement();
			statement.executeQuery("SELECT * FROM section WHERE code='" + code + "' AND year=" + year + " AND term=" + term);
			ResultSet result = statement.getResultSet();
			if(result.next()){
				statement.close();
				return true;
			}
			else{
				statement.close();
				return false;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	private String timeCollidedWith(String code, String year, String term){
		try{
			String time = null;
			Statement statement = connection.createStatement();
			ResultSet result = statement.executeQuery("SELECT timeslot FROM lecture WHERE code='" + code + "' AND year=" + year + " AND term=" + term);
			if(result.next())
				time = result.getString("timeslot");

			statement.executeQuery("SELECT code, timeslot FROM lecture WHERE year=" + year + " AND term=" + term + " AND code IN (" +
					"SELECT code FROM enroll WHERE sid=" + user + " AND year=" + year + " AND term=" + term + ")");
			result = statement.getResultSet();
			while(result.next()){
				if(result.getString("timeslot").equals(time))
					return result.getString("code");
			}
			statement.close();
			return null;
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}

	private boolean haveNotTakenCourse(String code, String year, String term){
		try{
			Statement statement = connection.createStatement();
			// the exact same section or different section of the course
			statement.executeQuery("SELECT * FROM enroll WHERE sid=" + user + " AND code='" + code + "' AND ((year=" + year + " AND term=" + term + ") OR (grade IS NOT NULL))");
			ResultSet result = statement.getResultSet();
			if(result.next()){
				statement.close();
				return false;
			}
			else{
				statement.close();
				return true;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	private int creditTakenInThisTerm(String year, String term){
		try{
			Statement statement = connection.createStatement();
			statement.executeQuery("SELECT credit FROM enroll, course WHERE sid=" + user + " AND year=" + year + " AND term=" + term + " AND enroll.code=course.code");
			ResultSet result = statement.getResultSet();
			int creditSum = 0;
			while(result.next()){
				creditSum += result.getInt("credit");
			}
			return creditSum;
		} catch (SQLException e) {
			e.printStackTrace();
			return -1;
		}
	}

	private void enrollCourse(String code, String year, String term){
		if(!courseExists(code)){
			System.out.println("There is no course with this course code.");
			return;
		}

		if(!sectionExists(code, year, term)){
			System.out.println("The course does not have this section.");
			return;
		}

		if(!prerequisiteMet(code, year, term)){
			System.out.println("Not all prerequisites are fulfilled.");
			return;
		}

		if(!haveNotTakenCourse(code, year, term)){
			System.out.println("You have already enrolled this course.");
			return;
		}

		String collidingCourseCode = timeCollidedWith(code, year, term);
		if(collidingCourseCode != null){
			System.out.println("There is a time conflict with the course " + collidingCourseCode);
			return;
		}

		try {
			Statement statement = connection.createStatement();
			int creditSum = creditTakenInThisTerm(year, term);

			ResultSet result = statement.executeQuery("SELECT credit FROM course WHERE code='" + code + "'");
			if(result.next()){
				if(creditSum + result.getInt("credit") > 21){
					System.out.println("You cannot take more than 21 credits.");
					statement.close();
					return;
				}
			}
			else{
				System.out.println("Course with code " + code + " does not exist.");
				statement.close();
				return;
			}

			int numRowsAffected = statement.executeUpdate("INSERT INTO enroll VALUES (" + user + ", '" + code + "', " + year + ", " + term + ", null)");
			statement.close();

			if(numRowsAffected == 0)
				System.out.println("Enrollment failed.");
			else
				System.out.println("Enrollment succeeded.");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private void dropCourse(String code, String year, String term){
		try {
			Statement statement = connection.createStatement();
			int numRowsAffected = statement.executeUpdate("DELETE FROM enroll WHERE sid=" + user + " AND code='" + code + "' AND year=" + year + " AND term=" + term + " AND grade IS NULL");
			statement.close();
			if(numRowsAffected == 0)
				System.out.println("Drop failed.");
			else
				System.out.println("Drop succeeded.");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private double gradeToPoint(String grade, int credit){
		double points = 0.0;
		if(grade == null)
			return 0.0;
		if(grade.equals("A"))
			points = 4.0 * credit;
		else if(grade.equals("B"))
			points = 3.0 * credit;
		else if(grade.equals("C"))
			points = 2.0 * credit;
		else if(grade.equals("D"))
			points = credit;
		else if(grade.equals("F"))
			points = 0.0;
		else if(grade.equals("null"))
			points = 0.0;

		return points;
	}

	private void showReport(String year, String term){
		int creditSum = 0;
		double pointSum = 0.0;
		Map<String, String> map = new TreeMap<String, String>();
		try{
			Statement statement = connection.createStatement();
			// results of this term
			statement.executeQuery("SELECT enroll.code, name, credit, grade FROM enroll, course WHERE enroll.code=course.code AND enroll.sid=" + user + " AND year=" + year + " AND term=" + term);
			ResultSet results = statement.getResultSet();

			System.out.println("_________________________________________________________________________________________________________________________________________________");
			System.out.println("| Course Code |                                         Course Name                                                  | Credits | Grade | Points |");
			System.out.println("|_____________|______________________________________________________________________________________________________|_________|_______|________|");
			while(results.next()){
				String code = results.getString(1);
				String name = results.getString("name");
				int credit = results.getInt("credit");
				String grade = results.getString("grade");
				double points = gradeToPoint(grade, credit);

				pointSum += points;
				creditSum += credit;

				map.put(code, String.format("  | %-100s | %-7d | %-5s | %-6.2f |", name, credit, grade, points));
			}

			for(Map.Entry entry : map.entrySet())
				System.out.println("|   " + entry.getKey() + entry.getValue());
			System.out.println("|_____________|______________________________________________________________________________________________________|_________|_______|________|");
			if(creditSum == 0)
				System.out.printf("| Term GPA: %.2f%129c\n", 0.0, '|');
			else
				System.out.printf("| Term GPA: %.2f%129c\n", pointSum/creditSum, '|');

			// results of previous terms
			statement.executeQuery("SELECT enroll.code, credit, grade FROM enroll, course WHERE enroll.code=course.code AND enroll.sid=" + user +
					" AND (year<" + year + " or (year=" + year + " and term<" + term + "))");
			results = statement.getResultSet();

			while(results.next()){
				int credit = results.getInt("credit");
				String grade = results.getString("grade");
				double points = gradeToPoint(grade, credit);

				creditSum += credit;
				pointSum += points;
			}
			if(creditSum == 0)
				System.out.printf("| Cumulative GPA: %.2f%123c\n", 0.0, '|');
			else
				System.out.printf("| Cumulative GPA: %.2f%123c\n", pointSum/creditSum, '|');
			System.out.println("|_______________________________________________________________________________________________________________________________________________|");

			statement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private void showInformation() {
		String[] tableNames = {"student", "course", "prerequisite", "section", "lecture", "enroll"};

		try {
			Statement statement = connection.createStatement();
			System.out.println("______________________________________");
			System.out.println("|         Table  | number of records |");
			System.out.println("|________________|___________________|");
			for (String tableName : tableNames) {
				statement.executeQuery("SELECT count(*) FROM " + tableName);
				ResultSet result = statement.getResultSet();
				if (result.next()) {
					System.out.printf("| %14s | %-17d |\n", tableName, result.getInt(1));
				}
			}
			System.out.println("|________________|___________________|");
			statement.close();
		} catch (SQLSyntaxErrorException e) {
			System.out.println("The tables does not exist.");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private void showEnrollmentRatings(String year, String term){
		Statement statement;
		String code;
		int quota;
		try {
			statement = connection.createStatement();
			statement.executeQuery("SELECT code, quota FROM section WHERE year=" + year + " AND term=" + term + " ORDER BY code");
			ResultSet codeQuota = statement.getResultSet();
			Map<String, Double> rates = new TreeMap<String, Double>();
			while (codeQuota.next()) {
				code = codeQuota.getString("code");
				quota = codeQuota.getInt("quota");
				Statement enrolls = connection.createStatement();
				enrolls.executeQuery("SELECT count(*) FROM enroll WHERE code='" + code + "' AND year=" + year + " AND term=" + term);
				ResultSet numEnrolled = enrolls.getResultSet();
				if (numEnrolled.next()) {
					int num = numEnrolled.getInt(1);
					rates.put(code, num / (double) quota);
				}
				enrolls.close();
			}

			statement.close();

			if (rates.size() == 0) {
				System.out.println("No enrollments in the specified year and term.");
				return;
			}

			// sort in descending order according to enrollment rates
			List<Double> valueList = new ArrayList<Double>(rates.values());
			Collections.sort(valueList);
			Collections.reverse(valueList);
			int rank = 0;
			double last = -1.0;

			System.out.println("________________________________________");
			System.out.println("| Rank | Course Code | Enrollment Rate |");
			System.out.println("|______|_____________|_________________|");
			while (valueList.size() > 0) {
				for (Map.Entry<String, Double> entry : rates.entrySet())
					if (entry.getValue().equals(valueList.get(0))) {
						if(entry.getKey() > last)
							rank++;
						System.out.printf("| %4d |   %8s  | %6.2f%10c|\n", rank, entry.getKey(), last, ' ');
						last = entry.getValue();
						rates.remove(entry.getKey());
						break;
					}

				valueList.remove(0);
			}
			System.out.println("|______|_____________|_________________|");

		} catch (SQLSyntaxErrorException e) {
			System.out.println("Tables does not exist.");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private void dropTables() {
		String dropEnroll = "DROP TABLE enroll CASCADE CONSTRAINTS";
		String dropLecture = "DROP TABLE lecture CASCADE CONSTRAINTS";
		String dropSection = "DROP TABLE section CASCADE CONSTRAINTS";
		String dropPrerequisite = "DROP TABLE prerequisite CASCADE CONSTRAINTS";
		String dropCourse = "DROP TABLE course CASCADE CONSTRAINTS";
		String dropStudent = "DROP TABLE student CASCADE CONSTRAINTS";

		try {
			Statement statement = connection.createStatement();
			statement.executeUpdate(dropEnroll);
			System.out.println("Table Enroll is dropped.");
			statement.executeUpdate(dropLecture);
			System.out.println("Table Lecture is dropped.");
			statement.executeUpdate(dropSection);
			System.out.println("Table Section is dropped.");
			statement.executeUpdate(dropPrerequisite);
			System.out.println("Table Prerequisite is dropped.");
			statement.executeUpdate(dropCourse);
			System.out.println("Table Course is dropped.");
			statement.executeUpdate(dropStudent);
			System.out.println("Table Student is dropped.");
			statement.close();
		} catch (SQLSyntaxErrorException e) {
			System.out.println("The tables does not exist.");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private void createTables() {
		String createStudent =
				"CREATE TABLE student(" +
						"sid NUMBER(10) PRIMARY KEY," +
						"name CHAR(25) NOT NULL," +
						"major CHAR(100) NOT NULL" +
						")";

		String createCourse =
				"CREATE TABLE course(" +
						"   code CHAR(8) PRIMARY KEY," +
						"   name CHAR(100) NOT NULL," +
						"   credit NUMBER(1) NOT NULL," +
						"   CHECK(credit >= 1 AND credit <= 3)" +
						")";

		String createPrerequisite =
				"CREATE TABLE prerequisite(" +
						"course CHAR(8)," +
						"precourse CHAR(8)," +
						"PRIMARY KEY(course, precourse)," +
						"FOREIGN KEY(course) REFERENCES course(code)," +
						"FOREIGN KEY(precourse) REFERENCES course(code)" +
						")";

		String createSection =
				"CREATE TABLE section(" +
						"code CHAR(8)," +
						"year NUMBER(4)," +
						"term NUMBER(1)," +
						"quota NUMBER(2) NOT NULL," +
						"instructor CHAR(25) NOT NULL," +
						"PRIMARY KEY(code, year, term)," +
						"FOREIGN KEY(code) REFERENCES course," +
						"CHECK(year > 0)," +
						"CHECK(term >= 1 AND term <= 3)," +
						"CHECK(quota > 0)" +
						")";

		String createLecture =
				"CREATE TABLE lecture(" +
						"code CHAR(8)," +
						"year NUMBER(4)," +
						"term NUMBER(1)," +
						"timeslot CHAR(3)," +
						"PRIMARY KEY(code, year, term, timeslot)," +
						"FOREIGN KEY(code, year, term) REFERENCES section(code, year, term)" +
						")";

		String createEnroll =
				"CREATE TABLE enroll(" +
						"sid NUMBER(10)," +
						"code CHAR(8)," +
						"year NUMBER(4)," +
						"term NUMBER(1)," +
						"grade CHAR(1)," +
						"PRIMARY KEY(sid, code, year, term)," +
						"FOREIGN KEY(sid) REFERENCES student(sid)," +
						"FOREIGN KEY(code, year, term) REFERENCES section(code, year, term)" +
						")";

		try {
			Statement statement = connection.createStatement();
			statement.executeUpdate(createStudent);
			System.out.println("Table Student is created.");
			statement.executeUpdate(createCourse);
			System.out.println("Table Course is created.");
			statement.executeUpdate(createPrerequisite);
			System.out.println("Table Prerequisite is created.");
			statement.executeUpdate(createSection);
			System.out.println("Table Section is created.");
			statement.executeUpdate(createLecture);
			System.out.println("Table Lecture is created.");
			statement.executeUpdate(createEnroll);
			System.out.println("Table Enroll is created.");
			statement.close();
		} catch (SQLSyntaxErrorException e) {
			System.out.println("The tables already exsit.");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private void insertStudentData() {
		try {
			BufferedReader fileBuffer = new BufferedReader(new FileReader(studentDataPath));
			String oneLine, insertValues;
			String[] tokens;
			Statement statement = connection.createStatement();
			int numberOfTuples = 0;

			while ((oneLine = fileBuffer.readLine()) != null) {
				tokens = oneLine.split("\t");
				// sid, name, major
				insertValues = "INSERT INTO student VALUES (" + tokens[0] + ", '" + tokens[1] + "', '" + tokens[2] + "')";
				statement.addBatch(insertValues);
				numberOfTuples += 1;
			}
			fileBuffer.close();

			int results[] = statement.executeBatch();
			statement.close();

			int numRowsAffected = 0;
			for (int row : results)
				numRowsAffected += row;

			System.out.println("Inserted " + numRowsAffected + " rows into table Student.");
			if (numRowsAffected != numberOfTuples)
				System.out.println((numberOfTuples - numRowsAffected) + " rows cannot be inserted.");

		} catch (FileNotFoundException e) { // new BufferedReader()
			System.out.println("The dataset file " + studentDataPath + " does not exist.");;
		} catch (IOException e) { // close()
			e.printStackTrace();
		} catch (SQLException e) { // createStatement()
			System.out.println("Cannot insert into Student.");
		}
	}

	private void insertCoursePrerequisiteData() {
		try {
			BufferedReader fileBuffer = new BufferedReader(new FileReader(courseDataPath));
			String oneLine, insertValues;
			String[] tokens;
			int[] results;
			Map<String, String[]> prerequisites = new HashMap<String, String[]>();
			Statement statement = connection.createStatement();
			int numberOfCourseTuples = 0, numberOfCoursesThatHavePrerequisite = 0;

			while ((oneLine = fileBuffer.readLine()) != null) {
				tokens = oneLine.split("\t");
				// code, name, credit
				insertValues = "INSERT INTO course VALUES ('" + tokens[0] + "', '" + tokens[1] + "', " + tokens[2] + ")";
				statement.addBatch(insertValues);
				numberOfCourseTuples += 1;

				// there are some prerequisites for this course
				if (tokens[3].equals("null") == false) {
					prerequisites.put(tokens[0], tokens[3].split(","));
					numberOfCoursesThatHavePrerequisite += 1;
				}
			}
			fileBuffer.close();

			// insert into Course
			results = statement.executeBatch();
			int numCourseRowsAffected = 0;
			for (int row : results)
				numCourseRowsAffected += row;

			System.out.println("Inserted " + numCourseRowsAffected + " rows into table Course.");
			if (numCourseRowsAffected != numberOfCourseTuples)
				System.out.println((numberOfCourseTuples - numCourseRowsAffected) + " rows cannot be inserted.");

			// insert into prerequisite
			statement.clearBatch();
			for (String course : prerequisites.keySet()) {
				for (String precourse : prerequisites.get(course)) {
					insertValues = "INSERT INTO prerequisite VALUES ('" + course + "', '" + precourse + "')";
					statement.addBatch(insertValues);
				}
			}

			results = statement.executeBatch();
			int numPrerequisiteRowsAffected = 0;
			for (int row : results)
				numPrerequisiteRowsAffected += row;

			System.out.println("Inserted " + numPrerequisiteRowsAffected + " rows into table Prerequisite.");
			System.out.println((numberOfCoursesThatHavePrerequisite) + " courses has prerequisites.");

			statement.close();
		} catch (FileNotFoundException e) { // new BufferedReader()
			System.out.println("The dataset file " + courseDataPath + " does not exist.");
		} catch (IOException e) { // close()
			e.printStackTrace();
		} catch (SQLException e) { // createStatement()
			System.out.println("Cannot insert into Course and Prerequisite.");
		}
	}

	private void insertSectionLectureData() {
		try {
			BufferedReader fileBuffer = new BufferedReader(new FileReader(sectionDataPath));
			String oneLine, insertValues;
			String[] tokens;
			int[] results;
			Map<String, String[]> lecture = new HashMap<String, String[]>();
			Statement statement = connection.createStatement();
			int numberOfSectionTuples = 0;

			while ((oneLine = fileBuffer.readLine()) != null) {
				tokens = oneLine.split("\t");
				String codeYearTerm = "'" + tokens[0] + "', " + tokens[1] + ", " + tokens[2];
				// code, year, term, quota, instructor
				insertValues = "INSERT INTO section VALUES (" + codeYearTerm + ", " + tokens[3] + ", '" + tokens[4] + "')";
				statement.addBatch(insertValues);
				numberOfSectionTuples += 1;

				lecture.put(codeYearTerm, tokens[5].split(","));
			}
			fileBuffer.close();

			// insert into Section
			results = statement.executeBatch();
			int numSectionRowsAffected = 0;
			for (int row : results)
				numSectionRowsAffected += row;

			System.out.println("Inserted " + numSectionRowsAffected + " rows into table Section.");
			if (numSectionRowsAffected != numberOfSectionTuples)
				System.out.println((numberOfSectionTuples - numSectionRowsAffected) + " rows cannot be inserted.");

			// insert into Lecture
			statement.clearBatch();
			for (String section : lecture.keySet()) {
				for (String timeslot : lecture.get(section)) {
					insertValues = "INSERT INTO lecture VALUES (" + section + ", '" + timeslot + "')";
					statement.addBatch(insertValues);
				}
			}

			results = statement.executeBatch();
			int numLectureRowsAffected = 0;
			for (int row : results)
				numLectureRowsAffected += row;

			System.out.println("Inserted " + numLectureRowsAffected + " rows into table Lecture.");

			statement.close();
		} catch (FileNotFoundException e) { // new BufferedReader()
			System.out.println("The dataset file " + sectionDataPath + " does not exist.");
		} catch (IOException e) { // close()
			e.printStackTrace();
		} catch (SQLException e) { // createStatement()
			System.out.println("Cannot insert into Section and Lecture.");
		}
	}

	private void insertEnrollData() {
		try {
			BufferedReader fileBuffer = new BufferedReader(new FileReader(enrollDataPath));
			String oneLine, insertValues;
			String[] tokens;
			Statement statement = connection.createStatement();
			int numberOfTuples = 0;

			while ((oneLine = fileBuffer.readLine()) != null) {
				tokens = oneLine.split("\t");
				if (tokens[4].equals("null") == false)
					tokens[4] = "'" + tokens[4] + "'";
				// sid, code, year, term, grade
				insertValues = "INSERT INTO enroll VALUES (" + tokens[0] + ", '" + tokens[1] + "', " + tokens[2] + ", " + tokens[3] + ", " + tokens[4] + ")";
				statement.addBatch(insertValues);
				numberOfTuples += 1;
			}
			fileBuffer.close();

			int results[] = statement.executeBatch();
			statement.close();

			int numRowsAffected = 0;
			for (int row : results)
				numRowsAffected += row;

			System.out.println("Inserted " + numRowsAffected + " rows into table Enroll.");
			if (numRowsAffected != numberOfTuples)
				System.out.println((numberOfTuples - numRowsAffected) + " rows cannot be inserted.");

		} catch (FileNotFoundException e) { // new BufferedReader()
			System.out.println("The dataset file " + enrollDataPath + " does not exist.");
		} catch (IOException e) { // close()
			e.printStackTrace();
		} catch (SQLException e) { // createStatement()
			System.out.println("Cannot insert into Enroll.");
		}
	}

	private void insertDataFromFile() {
		insertStudentData();
		insertCoursePrerequisiteData();
		insertSectionLectureData();
		insertEnrollData();
	}

	private void connectDatabase(String id, String pw) {
		try {
			Class.forName("oracle.jdbc.driver.OracleDriver");
			connection = DriverManager.getConnection(url, id, pw);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		System.out.println("Connection to Database is established.");
		System.out.println("Logged on as " + id);
	}

	private void disconnectDatabase() {
		try {
			if (connection.isClosed() == false)
				connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		System.out.println("Connection to Database is closed.");
	}

	public static void main(String[] args) {
		Scanner input = new Scanner(System.in);
		String inputString;
		boolean loggedIn = false;

		CourseEnrollmentSystem app = new CourseEnrollmentSystem();

		while (!loggedIn) {
			System.out.print("Please enter the user id: ");
			try {
				inputString = input.nextLine();
			} catch (NoSuchElementException e) {
				break;
			}
			if (inputString.equals("quit"))
				System.exit(0);
			loggedIn = app.login(inputString);

			app.run();
			loggedIn = false;
			app.logout();
		}
	}

	private void logout() {
		System.out.println("Logging off.");
		user = null;
	}

	private boolean login(String user) {
		if (user.equals("admin")) {
			this.user = "admin";
			System.out.println("Successfully logged on as " + user);
			return true;
		} else {
			try {
				Integer sid = Integer.parseInt(user);
			} catch (NumberFormatException e) {
				System.out.println(user + " is not a valid sid.");
				return false;
			}
			try {
				Statement statement = connection.createStatement();
				statement.executeQuery("SELECT sid FROM student WHERE sid = " + user);
				ResultSet result = statement.getResultSet();
				if (result.next())
					if (result.getInt("sid") == Integer.parseInt(user)) {
						this.user = user;
						System.out.println("Successfully logged on as " + user);
						return true;
					}

				System.out.println("Failed to log on. Such sid does not exist in the database.");
				return false;
			} catch (SQLSyntaxErrorException e) {
				System.out.println("Database Student does not exist.");
				System.out.println("Failed to log on. Such sid does not exist in the database.");
				return false;
			} catch (SQLException e) {
				e.printStackTrace();
				return false;
			}
		}
	}
}
