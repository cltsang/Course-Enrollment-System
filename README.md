### File Descriptions:

[1. Command line JAVA application]
- CourseEnrollmentSystem.java (java source file)
- dataset (raw data sets)
- ojdbc6.jar (JDBC driver)

[2. Web interface for timetable planning]
- timetable.php (web interface for user to access)
- TimetablePlanner_files (directory containing neccessary css and javascript file for .php file)

[3. Other files]
- manual.pdf (user manual for command and operation references)
- README.txt (this file)


### Compile Method:
Under UNIX environment (not Windows), 
type: javac CourseEnrollmentSystem.java


### Execute Method:

[For JAVA application]
After compilation,
type: java -classpath ./ojdbc6.jar:./ CourseEnrollmentSystem

[For web interface]
place them in the public_html file of our group server,
type "http://pc89123.cse.cuhk.edu.hk/~p006/timetable.php" in the internet browser
