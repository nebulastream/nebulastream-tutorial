import plotext as plt
import random, time, math
from pygtail import Pygtail

x, y, t = [], [], 0
max_p = 45
plt.plotsize(85, 16)

path = "./../../output/sink.csv"

try:
    for line in Pygtail(path):
        new_line = line.rstrip()

        x_str, y_str = new_line.split(",")   # split at comma

        try:
            x_val = float(x_str)
        except ValueError:
            # x_string = x_str  # fallback to string
            continue  # skip this line if x is not a float

        try:
            y_val = float(y_str)
        except ValueError:
            # y_string = y_str  # fallback to string
            continue  # skip this line if y is not a float
        # print(new_line)
        
        # x_val = int(x_str)
        # y_val = int(y_str)

        # print(f'X: {x_val:+.2f} | Y: {y_val:+.2f}')

        # t += 0.2
        # # Combine sine wave with random noise
        # value = 3 * math.sin(t) + random.uniform(-1, 1)
        x.append(x_val)
        y.append(y_val)
        
        if len(x) > max_p:
            x.pop(0)
            y.pop(0)
        
        plt.clt()
        plt.cld()
        plt.plot(x, y, color='magenta')
        plt.grid(True)
        plt.show()
        
        time.sleep(0.2)
        
except: print('🌀')