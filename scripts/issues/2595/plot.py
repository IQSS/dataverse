#!/usr/bin/env python
import sys
import numpy as np
import matplotlib.pyplot as plt
import matplotlib.dates as mdates
days, impressions = np.loadtxt("numconnacquired.tsv", delimiter='\t', skiprows=1, unpack=True,
            converters={ 0: mdates.strpdate2num('%Y-%m-%d %H:%M:%S.%f')})
plt.plot_date(x=days, y=impressions, fmt="r-")
plt.title("Number of logical connections acquired from the pool")
plt.ylabel("numconnacquired")
plt.grid(True)
plt.gcf().autofmt_xdate()
plt.savefig('out.png')
