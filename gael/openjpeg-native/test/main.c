#include <stdio.h>

extern void hide_openjpeg_get_header_from_file (const char *file);
int main (int argc, char*argv[])
{
   hide_openjpeg_get_header_from_file(argv[1]);
}
