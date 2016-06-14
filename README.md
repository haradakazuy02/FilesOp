# FilesOp

    It's a tool to operate files in order to build by command-line with source edited by NotePad.
    

## usage

- Install Simple Build Tool

- Build

    sbt pack

- Command

    target\pack\bin\file-op (options) [basedir] [command].. 

    - options
        - pathstarts [pre1,pre2,..] : the relative paths of target file must start with pre1, pre2, and so on.
        - excludestarts [pre1,pre2,..] : the relative paths of target file must not start with pre1, pre2, and so on.
        - pathends [ext1,ext2,..] : the relative paths of target file must end with pre1, pre2, and so on.
        - notonlyfile : target is not only file but alse directory.

    - cmmand..

        - path : show paths of target file

        - addcr (addcropt) : modify the line end to System.getProperty("line.separator")

            (addcropt)

            - encoding [enc] : specify the encoding with it read target files.

            -lf : modify line end to \n

            -crlf : modify line end to \r\n

        - rmbom :  remove the byte order mark of UTF-8.

        - remove : delete target files. use -notonlyfile to remove directory.

        - copy [todir] : copy target files to [todir].

        - command [command] : run command for each file. (the parameter is $path)
            ex. FilesOp -pathends txt . command "find \"xxx\" $path