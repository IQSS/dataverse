==========
Deployment
==========

Developers often only deploy the Dataverse Software to their :doc:`dev-environment` but it can be useful to deploy the Dataverse Software to cloud services such as Amazon Web Services (AWS).

.. contents:: |toctitle|
	:local:

Deploying the Dataverse Software to Amazon Web Services (AWS)
-------------------------------------------------------------

We have written scripts to deploy the Dataverse Software to Amazon Web Services (AWS) but they require some setup.

Install AWS CLI
~~~~~~~~~~~~~~~

First, you need to have AWS Command Line Interface (AWS CLI) installed, which is called ``aws`` in your terminal. Launching your terminal and running the following command to print out the version of AWS CLI will tell you if it is installed or not:

``aws --version``

If you have not yet installed AWS CLI you should install it by following the instructions at https://docs.aws.amazon.com/cli/latest/userguide/installing.html

Afterwards, you should re-run the "version" command above to verify that AWS CLI has been properly installed. If "version" still doesn't work, read on for troubleshooting advice. If "version" works, you can skip down to the "Configure AWS CLI" step.

Troubleshooting "aws: command not found"
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Please note that as of this writing the AWS docs are not especially clear about how to fix errors such as ``aws: command not found``. If the AWS CLI cannot be found after you followed the AWS installation docs, it is very likely that the ``aws`` program is not in your ``$PATH``. ``$PATH`` is an "environment variable" that tells your shell (usually Bash) where to look for programs.

To see what ``$PATH`` is set to, run the following command:

``echo $PATH``

On Mac, to update your ``$PATH`` to include the location where the current AWS docs install AWS CLI on the version of Python included with your Mac, run the following command:

``export PATH=$PATH:$HOME/Library/Python/2.7/bin``

After all this, you can try the "version" command again.

Note that it's possible to add an ``export`` line like the one above to your ``~/.bash_profile`` file so you don't have to run it yourself when you open a new terminal.

Configure AWS CLI
~~~~~~~~~~~~~~~~~

Next you need to configure AWS CLI.

Create a ``.aws`` directory in your home directory (which is called ``~``) like this:

``mkdir ~/.aws``

We will be creating two plain text files in the ``.aws`` directory and it is important that these files do not end in ".txt" or any other extension. After creating the files, you can verify their names with the following command:

``ls ~/.aws``

Create a plain text file at ``~/.aws/config`` with the following content::

        [default]
        region = us-east-1

Please note that at this time the region must be set to "us-east-1" but in the future we could improve our scripts to support other regions.

Create a plain text file at ``~/.aws/credentials`` with the following content::

        [default]
        aws_access_key_id = XXXXXXXXXXXXXXXXXXXX
        aws_secret_access_key = XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX

Then update the file and replace the values for "aws_access_key_id" and "aws_secret_access_key" with your actual credentials by following the instructions at https://aws.amazon.com/blogs/security/wheres-my-secret-access-key/

If you are having trouble configuring the files manually as described above, see https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-getting-started.html which documents the ``aws configure`` command.

Configure Ansible File (Optional)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

In order to configure Dataverse installation settings such as the password of the dataverseAdmin user, download https://raw.githubusercontent.com/GlobalDataverseCommunityConsortium/dataverse-ansible/master/defaults/main.yml and edit the file to your liking.

You can skip this step if you're fine with the values in the "main.yml" file in the link above.

Download and Run the "Create Instance" Script
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Once you have done the configuration above, you are ready to try running the "ec2-create-instance.sh" script to spin up a Dataverse installation in AWS.

Download `ec2-create-instance.sh`_ and put it somewhere reasonable. For the purpose of these instructions we'll assume it's in the "Downloads" directory in your home directory.

.. _ec2-create-instance.sh: https://raw.githubusercontent.com/GlobalDataverseCommunityConsortium/dataverse-ansible/master/ec2/ec2-create-instance.sh

To run it with default values you just need the script, but you may also want a current copy of the ansible `group vars <https://raw.githubusercontent.com/GlobalDataverseCommunityConsortium/dataverse-ansible/master/defaults/main.yml>`_ file.

ec2-create-instance accepts a number of command-line switches, including:

* -r: GitHub Repository URL (defaults to https://github.com/IQSS/dataverse.git)
* -b: branch to build (defaults to develop)
* -p: pemfile directory (defaults to $HOME)
* -g: Ansible GroupVars file (if you wish to override role defaults)
* -h: help (displays usage for each available option)

``bash ~/Downloads/ec2-create-instance.sh -b develop -r https://github.com/scholarsportal/dataverse.git -g main.yml``

You will need to wait for 15 minutes or so until the deployment is finished, longer if you've enabled sample data and/or the API test suite. Eventually, the output should tell you how to access the Dataverse installation in a web browser or via SSH. It will also provide instructions on how to delete the instance when you are finished with it. Please be aware that AWS charges per minute for a running instance. You may also delete your instance from https://console.aws.amazon.com/console/home?region=us-east-1 .

Caveat Recipiens
~~~~~~~~~~~~~~~~

Please note that while the script should work well on new-ish branches, older branches that have different dependencies such as an older version of Solr may not produce a working Dataverse installation. Your mileage may vary.


Migrating Datafiles from Local Storage to S3
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

A number of pilot Dataverse installations start on local storage, then administrators are tasked with migrating datafiles into S3 or similar object stores. The files may be copied with a command-line utility such as `s3cmd<https://s3tools.org/s3cmd>`. You will want to retain the local file hierarchy, keeping the authority (for example: 10.5072) at the bucket "root."

The below example queries may assist with updating dataset and datafile locations in the Dataverse installation's PostgresQL database. Depending on the initial version of the Dataverse Software and subsequent upgrade path, Datafile storage identifiers may or may not include a ``file://`` prefix, so you'll want to catch both cases.

To Update Dataset Location to S3, Assuming a ``file://`` Prefix
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

::

  UPDATE dvobject SET storageidentifier=REPLACE(storageidentifier,'file://','s3://')
    WHERE dtype='Dataset';

To Update Datafile Location to your-s3-bucket, Assuming a ``file://`` Prefix
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

::

  UPDATE dvobject
    SET storageidentifier=REPLACE(storageidentifier,'file://','s3://your-s3-bucket:')
    WHERE id IN (SELECT o.id FROM dvobject o, dataset s WHERE o.dtype = 'DataFile'
    AND s.id = o.owner_id AND s.harvestingclient_id IS null
    AND o.storageidentifier NOT LIKE 's3://%');

To Update Datafile Location to your-s3-bucket, Assuming no ``file://`` Prefix
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

::

  UPDATE dvobject SET storageidentifier=CONCAT('s3://your-s3-bucket:', storageidentifier)
	  WHERE id IN (SELECT o.id FROM dvobject o, dataset s WHERE o.dtype = 'DataFile'
	  AND s.id = o.owner_id AND s.harvestingclient_id IS null
	  AND o.storageidentifier NOT LIKE '%://%');


----

Previous: :doc:`coding-style` | Next: :doc:`containers`
