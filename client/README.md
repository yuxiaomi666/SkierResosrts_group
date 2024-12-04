# SkierClient-Assignment 2

## How to run the client
- If you want to send request to local:
  - In ```SkierClient.java```, find ```setBasePath(...)``` and comment out the other options except ````//local````
- If you want to send request to the single ec2 instance:
  - In ```SkierClient.java```, find ```setBasePath(...)``` and comment out the other options except ````//ec2````
- If you want to send request to the load balanced ec2 instances:
  - In ```SkierClient.java```, find ```setBasePath(...)``` and comment out the other options except ````//ALB````
- Then hit run. That's it!



## Other
- If there is any request failure:
  - first try run again.
  - If problem still exists, you
    could try changing the number of threads and number of requests sent in one single
    thread()  at line 17-21, some combination you could try:
    - NUM_THREADS_PHASE_2 = 84 & NUM_REQUESTS_PHASE_2 = 1000;


