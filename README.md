***Serenity JBehave test automation framework***

You can run scenarios in parallel.
Tests in JBehave we can divide by batches;
All tests are divided by quantity of batches; 
-Dparallel.agent.total –  quantity of all batches;
-Dparallel.agent.number – number of current batch;

To run all scenarios in 1 batch :
 - `mvn clean integration-test -Dparallel.agent.total=1 -Dparallel.agent.number=1 serenity:aggregate`
 
 To run all scenarios in X batches :
  `mvn clean integration-test -Dparallel.agent.total=X -Dparallel.agent.number={0...X} serenity:aggregate`
  
  more docs here : 
  **https://drive.google.com/file/d/0ByXXYqrGZKW9bXVxR3ZITmhERlE**
  
  To run tests you need Firefox version 29-38
