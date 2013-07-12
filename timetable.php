<html><head><meta http-equiv="Content-Type" content="text/html; charset=UTF-8">

<title>Timetable Planner</title>
<link rel="stylesheet" type="text/css" href="./TimetablePlanner_files/style.css">
<script type="text/javascript" src="./TimetablePlanner_files/jquery-1.7.min.js"></script>
<script type="text/javascript" src="./TimetablePlanner_files/script.js"></script>
</head>
<body>
<?php 
	function connectDB(){
		$dbstr = "(DESCRIPTION=
				(ADDRESS_LIST=
					(ADDRESS=
						(PROTOCOL=TCP)
						(HOST=db12.cse.cuhk.edu.hk)
						(PORT=1521)
					)
				)
				(CONNECT_DATA=
					(SERVER=DEDICATED)
					(SERVICE_NAME=db12.cse.cuhk.edu.hk)
				)
			)";
		$conn = oci_connect("a008", "tsqdoafg", $dbstr);
		if(!$conn){
			echo "ERROR: cannot establish the connection\n";
		}
		return $conn;
	}
?>

<div id="main">
	<div id="content">
		<div class="box">
			Year &amp; Term<br>
			<select id="yat" class="field">
				<option value="ns">-----------------------</option>
				<?php
					$conn=connectDB();
					$sql = "SELECT DISTINCT year, term FROM Section ORDER BY year DESC";
					$stid = oci_parse($conn, $sql);
					oci_execute($stid);
					while($row = oci_fetch_object($stid)){
						$year = $row->YEAR;
						$term = $row->TERM;
						$opTag = $year . "_" . $term;
						$opWord = $year . " Term " . $term;
						if(($_GET["year"]==$year) & ($_GET["term"]==$term)){
							echo "<option value=" . $opTag . " selected>" . $opWord . "</option>";
						} else {
							echo "<option value=" . $opTag . " >" . $opWord . "</option>";
						}
					}
					oci_close($conn);
				?>
			</select>
		</div>
		<?php 
			$courseChoosen=array();
			$courseSessions=array();
			$cChoosenList = "";
			$cSessionList = "";
			$tempResult=array();
			$canAdd="T";
			$canDrop="T";
			$tsMatching = array();
			$errorMsg ="";
		?>
		<div class="box">
			Course Code<br>
			<?php
				echo "<form action=\"timetable.php?year="
				.$_GET["year"]."&term=".$_GET["term"]."\" method=\"POST\">";
				
			?>
			<?php
				//Do the adding checking
				//Get the ChoosenList and put to array
				$cChoosenList = $_POST["courselist"];
				$token = strtok($cChoosenList, " ");
				while ($token != false)
				{
					$courseChoosen[] = $token;
					$token = strtok(" ");
				}
				//Get the course Session List and put to array
				$cSessionList = $_POST["courselistSession"];
				$token = strtok($cSessionList, " ");
				while ($token != false)
				{
					$courseSessions[] = $token;
					$token = strtok(" ");
				}
				
				//(add/drop) Check if the year and term has been inputed
				if($_POST["action"] == "add" || $_POST["action"] == "drop"){
					if(!($_GET["year"]) || !($_GET["term"]) ){
						$canAdd = "F";
						$canDrop = "F";
						$errorMsg ="<div id = \"error\">You have not specified the academic year and term</div>";
						//echo "<div id = \"error\">You have not specified the academic year and term</div>";
					}
				}
				
				//Check if user input nothing
				if(!(strtoupper($_POST["code"])) && (($_POST["action"]=="add") || ($_POST["action"]=="drop")) && ($canAdd != "F" || $canDrop!="F") ){
					if($_POST["action"] == "add"){
						$canAdd = "F";
						$errorMsg = "<div id = \"error\">No valid course section</div>";
						//echo "<div id = \"error\">No valid course section</div>";
					}
					else if($_POST["action"] == "drop"){
						$canDrop = "F";
						$errorMsg = "<div id = \"error\">The course has not been added</div>";
						//echo "<div id = \"error\">The course has not been added</div>";
					}
					
				}
				
				//Add: Check if the course is being added || Drop: Check if the course exists in our timetable
				if(strtoupper($_POST["code"]) && ($canAdd != "F" || $canDrop!="F")){
					//drop
					if($_POST["action"] == "drop"){
						$canDrop = "F";
					}
					//both
					for($i=0;$i<count($courseChoosen); $i++){
						if($courseChoosen[$i] == strtoupper($_POST["code"])){
							//drop implimentation to the case that we find the course code exist in our timetable
							if($_POST["action"] == "drop"){
								$canDrop = "T";
							}
							//add implimentation to the case that we find the course code exist in our timetable
							else if($_POST["action"] == "add"){
								$canAdd = "F";
								$errorMsg = "<div id = \"error\">The course has been added</div>";
								//echo "<div id = \"error\">The course has been added</div>";
							}
						}
					}

					//After Looping the hold courseChosen array, if there still do not exit any course which we want to drop
					if($_POST["action"] == "drop"){
						if($canDrop == "F"){
							$errorMsg = "<div id = \"error\">The course has not been added</div>";
							//echo "<div id = \"error\">The course has not been added</div>";
						}
					}
				}
				
				if($canAdd != "F" && ($_POST["action"] == "add")){
					$cTimeSlot = array();
					$conn=connectDB();
					$sql = "SELECT timeslot FROM Lecture WHERE code= '".strtoupper($_POST["code"])."' AND year = ".$_GET["year"]." AND term = ".$_GET["term"];
					$stid = oci_parse($conn, $sql);
					oci_execute($stid);
					while($row = oci_fetch_object($stid)){
						$cTimeSlot[]=$row->TIMESLOT;
					}
					oci_close($conn);
					
					//Loop if there exist any timeslot duplication between the table and the course new added
					for($i=0; $i<count($courseSessions);$i++){
						if($canAdd != "F"){
							//echo "$courseSessions[$i]<br/>";
							for($j=0;$j<count($cTimeSlot);$j++){
								$COMP1 = $courseSessions[$i]." ";
								$COMP2 = $cTimeSlot[$j];
								//echo "$COMP1 $COMP2 <br/>";
								if( $COMP1 == $COMP2 ){
									$canAdd = "F";
									$errorMsg = "<div id = \"error\">There is time collision</div>";
									//echo "<div id = \"error\">There is time collision</div>";
									break;
								}
							}
						}
					}
				}
				
				//Check if the course exist in such year and term (Only Add need to check this)
				if(strtoupper($_POST["code"]) && $canAdd != "F" && ($_POST["action"] == "add")){
					$conn=connectDB();
					$sql = "SELECT timeslot FROM Lecture WHERE code= '".strtoupper($_POST["code"])."' AND year = ".$_GET["year"]." AND term = ".$_GET["term"];
					$stid = oci_parse($conn, $sql);
					oci_execute($stid);
					//echo "code";
					if(!oci_fetch_object($stid) && code!=NULL){
						$canAdd = "F";
						$errorMsg = "<div id = \"error\">No valid course section</div>";
						//echo "<div id = \"error\">No valid course section</div>";
					}
					oci_close($conn);
				}
			
			?>

			
			<?php
				//print the code bar
				if($_POST["action"] == "add" && $canAdd == "F"){
					echo "<input type=\"input\" class=\"field\" name=\"code\" size=\"18\" maxlength=\"8\" value=".$_POST["code"].">";
				}
				else if($_POST["action"] == "add" && $canAdd == "T"){
					echo "<input type=\"input\" class=\"field\" name=\"code\" size=\"18\" maxlength=\"8\" value=\"\">";
				}
				else if($_POST["action"] == "drop" && $canDrop == "F"){
					echo "<input type=\"input\" class=\"field\" name=\"code\" size=\"18\" maxlength=\"8\" value=".$_POST["code"].">";
				}
				else if($_POST["action"] == "drop" && $canDrop == "T"){
					echo "<input type=\"input\" class=\"field\" name=\"code\" size=\"18\" maxlength=\"8\" value=\"\">";
				}
				else{
					echo "<input type=\"input\" class=\"field\" name=\"code\" size=\"18\" maxlength=\"8\" value=\"\">";
				}
				
			?>
				<input type="submit" name="action" value="add">
				<input type="submit" name="action" value="drop">

			
			<?php
				//Print the error message
				echo "$errorMsg";
				
				//If cannnot add, reset to the previous state
				if($canAdd == "F" || $canDrop == "F" ){
					$cTimeSlot = array();
					$cChoosenList = $_POST["courselist"];
					echo "<input type=\"hidden\" name=\"courselist\" value= \"" . $cChoosenList . "\">";
					
					$cSessionList = $_POST["courselistSession"];
					echo "<input type=\"hidden\" name=\"courselistSession\" value= \"" . $cSessionList . "\">";
				}
				
				//If the action is Add and the code pass all the error checking=============
				if($canAdd == "T" & ($_POST["action"]=="add") ){
					
					echo "<div id = \"success\">The course is added</div>";
					
					//add the new added course to courselist: e.g CSCI3170 -> "CSCI3150 CSCI3170"
					$cChoosenList = $_POST["courselist"] . " " . strtoupper($_POST["code"]);
					echo "<input type=\"hidden\" name=\"courselist\" value= \"" . $cChoosenList . "\">";
					$courseChoosen[] = strtoupper($_POST["code"]);			
					
					//add the new added courses' timeslots to the courselistSession
					$conn=connectDB();
					$sql = "SELECT timeslot FROM Lecture WHERE code= '".strtoupper($_POST["code"])."' AND year = ".$_GET["year"]." AND term = ".$_GET["term"];
					$stid = oci_parse($conn, $sql);
					oci_execute($stid);
					$cSessionList = $_POST["courselistSession"];
					while($row = oci_fetch_object($stid)){
						$tempResult[]=$row->TIMESLOT;
						//Remove the space from the data record timeslot
						$token = strtok($row->TIMESLOT, " ");
						$tsMatching[$token]=strtoupper($_POST["code"]);
						$courseSessions[] = $token;
						//echo "$token=$tsMatching[$token]<br/>";
						$cSessionList = $cSessionList . " " . $row->TIMESLOT;
					}
					oci_close($conn);
					echo "<input type=\"hidden\" name=\"courselistSession\" value= \"" . $cSessionList . "\">";
				}
				//If the action is Drop and the code pass all the error checking==============
				else if($canDrop == "T" & ($_POST["action"]=="drop") ){
					echo "<div id = \"success\">The course is dropped</div>";
					//Redefine the courseList string
					$tempString ="";
					$codeIndex = 0;
					for($i=0; $i<count($courseChoosen); $i++){
						//Concate the tempString
						if($courseChoosen[$i] != strtoupper($_POST["code"])){
							$tempString = $tempString . " " . $courseChoosen[$i];
						}
						else{
							//mark the position of the index of the array which we need to delete
							$codeIndex = $i;
						}
					}
					//reset the courseChoosen array
					unset($courseChoosen[$codeIndex]);
					$courseChoosen = array_values($courseChoosen);
					//Redefine the courseList
					$cChoosenList = $tempString;
					echo "<input type=\"hidden\" name=\"courselist\" value= \"" . $cChoosenList . "\">";
					
					//Remove the selected courses' timeslots from the courselistSession
					//Get the course's timeslots from the database
					$conn=connectDB();
					$sql = "SELECT timeslot FROM Lecture WHERE code= '".strtoupper($_POST["code"])."' AND year = ".$_GET["year"]." AND term = ".$_GET["term"];
					$stid = oci_parse($conn, $sql);
					oci_execute($stid);
					//$cSessionList = $_POST["courselistSession"];
					$tempResult = array();
					while($row = oci_fetch_object($stid)){
						//Remove the space from the data record timeslot
						$token = strtok($row->TIMESLOT, " ");
						$tempResult[] = $token;

					}
					oci_close($conn);
					//Redefine the array
					$removeIndex = array();
					for($i=0; $i<count($courseSessions); $i++){
						for($j=0; $j<count($tempResult);$j++){
							if($courseSessions[$i] == $tempResult[$j]){
								$removeIndex[] = $i;
							}
						}
					}
					//print_r($removeIndex);
					
					//reset the courseSessions array
					for($i=0; $i<count($removeIndex);$i++){
						unset($courseSessions[$removeIndex[$i]]);
					}
					$courseSessions = array_values($courseSessions);
					//print_r($courseSessions);
					
					//Loop the current course Session array and redefine the new string of 
					$tempString="";
					for($i=0; $i<count($courseSessions); $i++){
						if($i==0){
							$tempString = $tempString . $courseSessions[$i];
						}
						else{
							$tempString = $tempString ." ". $courseSessions[$i];
						}
					}
					//echo $tempString;
					
					//Redefine the cSessionList
					$cSessionList = $tempString;			
					echo "<input type=\"hidden\" name=\"courselistSession\" value= \"" . $cSessionList . "\">";
					
					
				}
			?>
			</form>
		</div>
		
		<div class="box">
			<table width="100%">
				<tbody><tr>
					<th width="20%"></th>
					<th width="16%">Mon</th>
					<th width="16%">Feb</th>
					<th width="16%">Wed</th>
					<th width="16%">Thu</th>
					<th width="16%">Fri</th>
				</tr>
				
				<?php 
					$cTimeSlot = array();
					$conn=connectDB();
					$sql = "SELECT timeslot FROM lecture WHERE code= '".strtoupper($_POST["code"])."' AND year = ".$_GET["year"]." AND term = ".$_GET["term"];
					$stid = oci_parse($conn, $sql);
					oci_execute($stid);
					while($row = oci_fetch_object($stid)){
						# array
						//echo $cTimeSlot[]=$row->TIMESLOT;
						$timeTable[$row->TIMESLOT] = strtoupper($_POST["code"]);
						//echo $ttttable[$row->TIMESLOT];
					}
					oci_close($conn);
				?>
				
				<?php 
				$hours = '08';
				$houre = '09';
				#row=time
				for($r=1; $r<=10; $r++){
					echo "<tr><td class=\"time\">".$hours.":30-".$houre.":15</td>";
					#col=day
					for($c=1; $c<=5; $c++){
						echo "<td>";
						
						# check already added course
						
						# check time collision(to-do)
						
						# print (multiple) code in match time slot
						
						switch($c){
							case 1:	$currSlot = 'M'.$r." ";	break;
							case 2: $currSlot = 'T'.$r." ";	break;
							case 3: $currSlot = 'W'.$r." ";	break;
							case 4: $currSlot = 'H'.$r." ";	break;
							case 5: $currSlot = 'F'.$r." ";	break;
							default: echo "\nWrong timeslot!!\n";
						}
						
						//echo $timeTable[$currSlot];
						//Find and show the timeslot
						//if($_POST["action"]=="add"){
							$tempResult = array();
							$conn=connectDB();
							$sql = "SELECT code FROM Lecture WHERE timeslot= '". $currSlot ."' AND year = ".$_GET["year"]." AND term = ".$_GET["term"];
							$stid = oci_parse($conn, $sql);
							oci_execute($stid);
							
							while($row = oci_fetch_object($stid)){
								$tempResult[]=$row->CODE;
							}
							oci_close($conn);
							for($i=0;$i<count($courseSessions);$i++){
								if(($courseSessions[$i]." ")==$currSlot){
									//Find the course code
									for($j=0; $j<count($tempResult); $j++){
										for($k=0;$k<count($courseChoosen); $k++){
											if( $courseChoosen[$k] == $tempResult[$j] ){
												echo "$courseChoosen[$k]";

											}
										}
									}
								}
							}
						//}

						echo "</td>";
					}
					$hours++;
					$houre++;
					echo "</tr>";
				}
				?>
			</tbody></table>
		</div>
		
		
		<br>
		<div class="box">Course list</div>
		<?php
			if(empty($courseChoosen)){
				echo "<div class=\"box2\">No course is added</div>";
			} else {
				//[csci3170 CSCI3180 IERG1000 ]
				$tempString ="";
				for($i=0;$i<count($courseChoosen); $i++){
					if($i!=(count($courseChoosen)-1)){
						$tempString = $tempString . "C.code = '" . $courseChoosen[$i] . "' OR ";
					}
					else{
						$tempString = $tempString . "C.code = '" . $courseChoosen[$i] . "'";
					}
				}
				//print_r($courseChoosen);
				$conn=connectDB();
					$sql = "SELECT DISTINCT C.code, C.name, C.credit FROM Lecture L, Course C
							WHERE C.code = L.code AND (".$tempString.") AND year = ".$_GET["year"]." AND term = ".$_GET["term"].
							" ORDER BY code ASC";
					//echo "$sql<br>";
					$stid = oci_parse($conn, $sql);
					oci_execute($stid);
					while($row = oci_fetch_object($stid)){
						$ccode = $row->CODE;
						$cname = $row->NAME;
						$credit = $row->CREDIT;
						
						if($credit!=1){
							echo "<div class=\"box2\">".$ccode." ".$cname." (".$credit." credits)</div>";
						} else {
							echo "<div class=\"box2\">".$ccode." ".$cname." (".$credit." credit)</div>";
						}
					}
					oci_close($conn);
			}
		?>
		
		
		
		
		
	</div>
</div>


</body></html>

