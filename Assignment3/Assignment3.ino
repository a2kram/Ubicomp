#include <Adafruit_CircuitPlayground.h>
#include <Adafruit_Circuit_Playground.h>


#define STEP_COUNT_THRESHOLD  9.9
#define STEP_COUNT_STATIONARY 9.4

#define LPF_ZEROS 1
#define LPF_POLES 1
#define LPF_GAIN  1.601528487e+02

//#define SERIAL_OUT


static float run_lpf (float value)
{
  static float xv[LPF_ZEROS + 1] = {0};
  static float yv[LPF_POLES + 1] = {0};

  xv[0] = xv[1]; 
  xv[1] = value / LPF_GAIN;
  yv[0] = yv[1]; 
  yv[1] = (xv[0] + xv[1]) + (0.9875119299  * yv[0]);
  
  return yv[1];
}

void setup () 
{
#ifdef SERIAL_OUT
  Serial.begin(9600);
#endif
  
  CircuitPlayground.begin ();
}

void loop () 
{
  int step_count = 0;
  bool above = false;
  float ax, ay, az, a_mag, filtered_mag;
 
  while (1)
  {    
    if (CircuitPlayground.leftButton() || CircuitPlayground.rightButton())
    {
      step_count = 0;
      above = false;     
      set_neopixel_leds (0);
    }
    
    ax = CircuitPlayground.motionX();
    ay = CircuitPlayground.motionY();
    az = CircuitPlayground.motionZ();
    a_mag = sqrt (ax * ax + ay * ay + az * az);
  
    filtered_mag = run_lpf (a_mag);
  
    if (filtered_mag > STEP_COUNT_THRESHOLD)
    {
      CircuitPlayground.setPixelColor (9, 0, 0, 255);
      above = true;
    }
    
    if (filtered_mag < STEP_COUNT_STATIONARY)
    {
      if (above)
      {
        ++step_count;
        set_neopixel_leds (step_count);
      }

      
      CircuitPlayground.setPixelColor (9, 0, 255, 0);
      above = false;
    }

#ifdef SERIAL_OUT
    Serial.print ("filtered_mag: ");
    Serial.println (filtered_mag);
#endif
  }
}

void set_neopixel_leds (uint16_t value) 
{
  uint16_t n_led = 0;
  
  CircuitPlayground.clearPixels ();

  while ((n_led < 8) && (value != 0))
  {
    if (value & 0x01) 
    {
      CircuitPlayground.setPixelColor (n_led, 255, 255, 255);
    }

    value >>= 1;
    ++n_led;  
  }
}

