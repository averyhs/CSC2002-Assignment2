#!/bin/bash

FDIR="io-files/"

echo "Name of input file in ./io-files"
read filename

java -cp ./bin flow.Flow $FDIR$filename
