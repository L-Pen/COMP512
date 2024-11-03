#!/bin/bash

rm game* 
./build_tiapp.sh
./comp512st/tests/run_tiapp_auto.sh

echo "number of moves for 1"
grep "player number 1" game-30-99-1.log | wc -l

echo "number of moves for 2"
grep "player number 2" game-30-99-1.log | wc -l

echo "number of moves for 3"
grep "player number 3" game-30-99-1.log | wc -l

echo "diff between 1 and 2"
diff game-30-99-1.log game-30-99-2.log | grep -v "Game" | grep -e "<" -e ">" -e "|" | wc -l

echo "diff between 1 and 3"
diff game-30-99-1.log game-30-99-3.log | grep -v "Game" | grep -e "<" -e ">" -e "|" | wc -l

echo "diff between 2 and 3"
diff game-30-99-2.log game-30-99-3.log | grep -v "Game" | grep -e "<" -e ">" -e "|" | wc -l
