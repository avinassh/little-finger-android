# Little Finger Little Finger Android

This repo contains [Little Finger](http://avi.im/little-finger) Android library.

## Usage

To install, add this to your project's build gradle:

    allprojects {
        repositories {
            jcenter {
                url "http://jcenter.bintray.com/"
            }
            maven {
                url "https://dl.bintray.com/avinassh/little-finger"
            }
        }
    }

Then in app's build gradle:

    compile 'im.avi:littlefinger:0.1.2'

then you can import it:  

    import im.avi.littlefinger.LittleFinger;

and call it:

    LittleFinger.start(this, "https://your-heroku-app.heroku.com");


## Example

Your `MainActivity.java` code can be like this:

    import im.avi.littlefinger.LittleFinger;

    public class MainActivity extends AppCompatActivity {

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

            // your code
            LittleFinger.start(this, "https://your-heroku-app.heroku.com");
        }

         // rest of the code
    }


## License

The mighty MIT license. Please check `LICENSE` for more details.
