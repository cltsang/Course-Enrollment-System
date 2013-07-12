$(document).ready(function(){
	$("select#yat").change(function(){
		var yat = $("select#yat").val();
		if(yat == "ns"){
			window.location = "timetable.php";
		}else{
			var val = yat.split("_"); 
			window.location = "timetable.php?year=" + val[0] +  "&term=" + val[1];
		}
	});
});
