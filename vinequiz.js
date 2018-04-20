$(document).ready(function() 
{    $("#results").click(function() {                

if (!$("input[@name=q1]:checked").val() ||            
 {            
alert("You're not done yet!");        
}        

else {            
var cat1name = "1";            
;            
            

var cat1 = ($("input[@name=q1]:checked").val() != "a"); 
           
var categories = [];                        

if (cat1) { categories.push(cat1name) };            
                  

var catStr = 'You answered the following questions incorrectly: ' + categories.join(', ') + '';                     
$("#categorylist").text(catStr);                        
$("#categorylist").show("slow");            

if (cat1) { $("#category1").show("slow"); };            

{ $("#closing").show("slow"); };
}
    });});
