# Player Service Model

This is a thin model wrapper container based on `Player.csv` data.

To build and run:
```shell
docker build -t a4a_model .
docker run -d -p 5000:5000 a4a_model
```

This will expose port 5000.

To send an inference request to the AI model using a seed from the database:
```shell
$ curl -H "Content-type: application/json" -d '{"seed_id":"abbotji01","team_size":10}' http://127.0.0.1:5000/team/generate
{"seed_id":"abbotji01","team_size":10,"member_ids":["abbotji01","combspa01","maurero01","cummijo01","flemida01","macdobo01","eddych01","morriha02","mcgrifr01","blossgr01"]}
```

To send an inference request to the AI model using a set of features:
```shell
$ curl -H "Content-type: application/json" -d '{"features":{"birth_year":1970, "height":70, "weight":120, "bats":"R", "throws":"L"},"team_size":10}' http://127.0.0.1:5000/team/generate
{"seed_id":null,"prediction_id":"ddedf511-2e68-4ab5-87c5-c77b8d15eb23","team_size":10,"member_ids":["roblevi01","deverra01","goharlu01","albieoz01","barrefr02","urenari01","uriasju01","verdual01","mejiafr01","sierrma01"]}
```
Note that the features are optional, and features that are not provided will be assumed to be the mean values in the training dataset. The unit of weight is pounds. The unit of height is inches. Batting and throwing may be right handed (`R`), left handed (`L`) or no preference (`N`).

To send feedback about the recommendations for a prior seed:
```shell
$ curl -H "Content-type: application/json"  -d '{"seed_id":"abbotji01","member_id":"maurero01","feedback":-1}' http://127.0.0.1:5000/team/feedback 
{"seed_id":"abbotji01","member_id":"maurero01","accepted":true}
```