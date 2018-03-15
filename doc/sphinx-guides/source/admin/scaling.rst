Scaling
=======

As usage of your Dataverse installation grows, you may need to start scaling it up in various dimensions. Scaling is a complex topic feedback from the Dataverse community is very welcome.

.. contents:: Contents:
	:local:

Load Balancing
--------------

FIXME: Write something about load balancing. The :doc:`/installation/prep` section of the Installation Guide mentions load balancers under "Advanced Installation".

Rate Limiting
-------------

Rate limiting means that requests are denied or slowed down after a certain threshold has been reached. For example, if the same IP address makes over 2000 requests in 5 minutes, that IP address could be denied for an hour.

Rate limiting strategies are platform-specific but to use AWS as an example, Web Application Firewall (WAF) can be configured with a "Rate-based Rules" to dynamically block or unblock IP addresses when requests they are sending go above and below a certain threshold. See https://aws.amazon.com/blogs/aws/protect-web-sites-services-using-rate-based-rules-for-aws-waf/

Other Scaling Topics
--------------------

What does scaling mean to you? What should be added to this section on scaling?
