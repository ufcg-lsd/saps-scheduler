<<<<<<< HEAD
#newScheduler
=======
# Install and Configure Scheduler

The SAPS Scheduler component is responsible for selecting tasks (to this end, it interacts with the SAPS Catalog component) to be processed by the Workers managed by the Arrebol Service.

## Dependencies

In an apt-based Linux distro, type the below commands to install the Scheduler dependencies.

```bash
sudo apt-get update
sudo apt-get -y install openjdk-8-jdk
sudo apt-get -y install maven
sudo apt-get -y install git
```

In addition to the installation of the above Linux packages, the Scheduler source code should be fetched from its repository and compiled, however there are two dependency repositories: saps-common and saps-catalog. This could be done following the below steps:

```bash
# saps-common repository
git clone https://github.com/ufcg-lsd/saps-common
cd saps-common
git checkout develop
mvn install

# saps-catalog repository
git clone https://github.com/ufcg-lsd/saps-catalog
cd saps-catalog
git checkout develop
mvn install

# saps-scheduler repository
git clone https://github.com/ufcg-lsd/saps-scheduler
cd saps-scheduler
git checkout develop
mvn install
```

## Configure

Edit the files:
- [Scheduler configuration file](/config/scheduler.conf) to allow its comunication with the SAPS Catalog and Arrebol Service. This configuration file also customizes the behaviour of the Scheduler, including the frequency that the Scheduler tries to select new task to be submitted.
- [SAPS Scripts](/resources/execution_script_tags.json) to make available new versions of the algorithms, for the three steps of the SAPS workflow (input downloading, preprocessing and processing). Any new algorithm should be packed as a docker image. See below example on how to specify the algorithms:

```json
{
"inputdownloading":[
	{
	"name": "$name_inputdownloading_option1",
	"docker_tag": "$docker_tag_inputdownloading_option1",
	"docker_repository": "$docker_repository_inputdownloading_option1"
	}
],
"preprocessing":[
	{
	"name": "$name_preprocessing_option1",
	"docker_tag": "$docker_tag_preprocessing_option1",
	"docker_repository": "$docker_repository_preprocessing_option1"
	}
],
"processing":[
	{
	"name": "$name_processing_option1",
	"docker_tag": "$docker_tag_processing_option1",
	"docker_repository": "$docker_repository_processing_option1"
	},
	{
	"name": "$name_processing_option2",
	"docker_tag": "$docker_tag_processing_option2",
	"docker_repository": "$docker_repository_processing_option2"
	}
]
}
```

## Run

Once the configuration file is edited, the below commands are used to start and stop the Scheduler component.

```bash
# Start command
bash bin/start-service
```

```bash
# Stop command
bash bin/stop-service
```
>>>>>>> develop
