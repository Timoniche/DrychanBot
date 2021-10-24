# Dating Bot For Drychan

![alt text](https://github.com/Timoniche/DrychanBot/blob/main/src/main/resources/photos/logo.JPEG)

## Profiles DB scheme:

![alt text](https://github.com/Timoniche/DrychanBot/blob/main/src/main/resources/photos/drychanScheme.png)

## Pipeline:

![alt text](https://github.com/Timoniche/DrychanBot/blob/main/src/main/resources/photos/pipeline.png)

## How to run locally?

`ngrok http 8080`

![alt text](https://github.com/Timoniche/DrychanBot/blob/main/src/main/resources/photos/vkOptions.png)

**Properties:**

```
-DPGAAS_URL=jdbc:postgresql://localhost:5432/${DB_NAME}

-DPGAAS_USERNAME=${USER_NAME}

-DPGAAS_PASSWORD=${PASSWORD}

-DTOKEN=c6414558667...634bf26fa3...c56e24

-DGROUP_ID=8...002

-DCONFIRMATION_CODE=12....86
```

## How to run with docker compose?

1) fill properties in the .env file:
   https://github.com/Timoniche/DrychanBot/blob/main/src/main/docker/.env
2) run `sudo bash scripts/build_maven_docker_and_run.sh`
or `sudo bash scripts/build_docker_and_run.sh` if jar exists