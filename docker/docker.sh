#!/bin/bash

readonly REPOSITORY=ufcgsaps/common
readonly USAGE="usage: docker.sh {build|push|publish} <TAG>"
readonly MY_PATH=$(cd "$(dirname "${0}")" || { echo "For some reason, the path is not accessible"; exit 1; }; pwd )
readonly WORKING_DIRECTORY="$(dirname "${MY_PATH}")"
readonly DOCKER_FILE_PATH="${MY_PATH}/Dockerfile"

build() {
  local TAG="${1-latest}"
  docker build --tag "${REPOSITORY}":"${TAG}" \
            --file "${DOCKER_FILE_PATH}" "${WORKING_DIRECTORY}"
}

push() {
  local TAG="${1-latest}"
  docker push "${REPOSITORY}":"${TAG}"
}

main() {
  if [ "$#" -eq 0 ]; then
      echo "${USAGE}"
      exit 1
  fi
  case ${1} in
    build) shift
      build "$@"
      ;;
    push) shift
      push "$@"
      ;;
    publish) shift
      build "$@"
      push "$@"
      ;;
    *)
      echo "${USAGE}"
      exit 1
  esac
}

main "$@"