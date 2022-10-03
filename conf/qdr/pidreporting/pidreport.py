import smtplib, ssl, datetime, os.path

def numSort(s):
    return int(s[0:s.index("_")])

currentmonth = (datetime.datetime.now().replace(day=1) - datetime.timedelta(days=1)).strftime("%Y-%m")
filename = "/srv/glassfish/dataverse/logs/PIDFailures_" + currentmonth + ".log"
receivers = "comma-separated-email-list"  # Enter receiver address
message= "Subject: PID Failure Report for " + currentmonth +"\nTo: " + receivers + "\n\n"

if os.path.exists(filename):
    d={}

    with open(filename) as f:
        for line in f.readlines()[1:]:
            (pid, uri, method, ip, time)=line.split("\t")
            if pid not in  d:
                d[pid] = []
            d[pid].append(method + " " + uri + " from " + ip + " at " + time)
    l=[]
    for key in d:
        l.append(str(len(d[key])) + "_" + key)

    l.sort(reverse=True, key=numSort)

    message = message + "Hits\tDOI\tURI\n(Note: clicking links will record new failures unless these are drafts)\n"

    for val in l:
        doi = val[val.index("_")+1:]
        message = message + "\n" + str(numSort(val)) + "\t" + doi + "\t" + "https://data.qdr.syr.edu/dataset.xhtml?persistentId=" + doi

    message = message + "\n\nDetails:\n\n"

    for val in l:
        doi = val[val.index("_")+1:]
        message = message + doi + "\n\t" + "\n\t".join(d[doi]) + "\n"
else:
    message= message + "No Failures this month\n\n"

port = 465  # For SSL
smtp_server = "email-smtp.us-east-1.amazonaws.com"
sender_email = "sender email"  # Enter your address
username = "username"
password = "password"
#password = input("Type your password and press enter: ")

context = ssl.create_default_context()
with smtplib.SMTP_SSL(smtp_server, port, context=context) as server:
    server.login(username, password)
    server.sendmail(sender_email, receivers.split(","), message)
