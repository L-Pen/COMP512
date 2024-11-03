#!/bin/bash

rm game* 
./build_tiapp.sh
./comp512st/tests/run_tiapp_auto.sh

num_players=$1

for i in $(seq 1 "$num_players");
do
    echo "number of moves for $i"
    grep "player number $i" game-30-99-$i.log | wc -l
done

for i in $(seq 1 "$((num_players - 1))");
do
    for j in $(seq $((i + 1)) "$num_players");
    do
        echo "diff between $i and $j"
        diff game-30-99-$i.log game-30-99-$j.log | grep -v "Game" | grep -e "<" -e ">" -e "|" | wc -l
    done
done