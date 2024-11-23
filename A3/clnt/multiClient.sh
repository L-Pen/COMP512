#!/bin/bash

num_runs=$1

for i in $(seq 1 "$num_runs");
do
    ./runclnt.sh 5000000 &
done