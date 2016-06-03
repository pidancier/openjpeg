# run test including valgind report
JP2K_FILE_PATH=`pwd`/test.jp2

CURRENT_DIR=`pwd`
cd ../target/
make clean
make
cp bin/libopenjp2.so $CURRENT_DIR
cd $CURRENT_DIR
gcc -g main.c -o test -L. -lopenjp2
declare -x LD_LIBRARY_PATH=$CURRENT_DIR
valgrind --leak-check=yes  ./test $JP2K_FILE_PATH
