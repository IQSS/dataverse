from config import (ConfigSectionMap)
from subprocess import Popen, PIPE, STDOUT
from time import (time)
from datetime import (datetime)

def send_notification(text):
    try: 
        notification_address = ConfigSectionMap("Notifications")['email']
    except:
        notification_address = None

    if (notification_address is None):
        raise ValueError('Notification email address is not configured')

    nowdate_str = datetime.fromtimestamp(time()).strftime('%Y-%m-%d %H:%M')
    subject_str = ('Dataverse datafile backup report [%s]' % nowdate_str)

    p = Popen(['mail','-s',subject_str,notification_address], stdout=PIPE, stdin=PIPE, stderr=PIPE)
    stdout_data = p.communicate(input=text)[0]

def main():
    send_notification('backup report: test, please disregard')

if __name__ == "__main__":
    main()
