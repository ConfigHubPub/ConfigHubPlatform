.. _python_client:

Python
^^^^^^

This is a helper script to get you started with the Python configuration pull requests.

.. code-block:: python

    #!/usr/bin/python

    # ConfigHub API for configuration and files stored in a repository.
    # -h option gives usage.

    import httplib
    import sys, getopt
    import json
    import os
    import errno

    token = ''
    context = ''
    outfile = ''
    appName = ''
    files = {}
    noProps = ''
    serverUrl = 'confighubHost'
    version = "v1.0"

    print (sys.version)

    def main(argv):
        try:
            opts, args = getopt.getopt(argv,
                                       "hc:t:o:a:v:f:np",
                                       ["context=", "token=", "fileout=", "appName=", "version=", "file=", "no-props"])

        except getopt.GetoptError:
            print 'test.py -c <context> -t <token> -o <fileout> -a <appName> -v <version> -f <repoFileName > localfileName> -np <no-props>'
            sys.exit(2)

        for opt, arg in opts:
            if opt == '-h':
                print 'test.py -c <context> -t <token> -o <fileout> -a <appName> -v <version> -f <repoFileName > localfileName> -np <no-props>'
                sys.exit()
            elif opt in ("-t", "--token"):
                global token
                token = arg.strip()
            elif opt in ("-c", "--context"):
                global context
                context = arg.strip()
            elif opt in ("-o", "--fileout"):
                global outfile
                outfile = arg.strip()
            elif opt in ("-a", "--appName"):
                global appName
                appName = arg
            elif opt in ("-a", "--appName"):
                global version
                version = arg.strip()
            elif opt in ("-f", "--file"):
                global files
                fin,fout = arg.split('>')
                files[fin.strip()] = fout.strip()
            elif opt in ("-u", "--url"):
                global serverUrl
                serverUrl = arg.strip()
            elif opt in ("-np", "--no-props"):
                global noProps
                noProps = 'true'

    if __name__ == "__main__":
        main(sys.argv[1:])

    if (token == '') or (context == ''):
        print("Token and context must be specified")
        sys.exit()

    filesToGet = ",".join(list(files.keys()))

    headers = {
        'Client-Token': token,
        'Context': context,
        'Client-Version': version,
        'Application-Name': appName,
        'Files': filesToGet,
        'No-Properties': noProps,
    }

    conn = httplib.HTTPSConnection(serverUrl)
    conn.request("GET", "/rest/pull", {}, headers)

    r1 = conn.getresponse()
    jc = r1.read()
    conn.close()

    jsonConfig = json.loads(jc)

    for repoFileName in jsonConfig['files']:
        localFileName = files[repoFileName]

        try:
            os.makedirs(os.path.dirname(localFileName))
        except OSError as e:
            if e.errno != errno.EEXIST:
                raise

        with open(localFileName, "w") as f:
            f.write(jsonConfig['files'][repoFileName])

    if outfile != '':
        try:
            os.makedirs(os.path.dirname(outfile))
        except OSError as e:
            if e.errno != errno.EEXIST:
                raise

        del jsonConfig['files']
        with open(outfile, 'w') as outfile:
            json.dump(jsonConfig, outfile, indent=4)


