
LayTrax is an Android application for making a smart phone function as a GPS location tracking device. The application stores
location information in a cloud database, where both current and historical locations can be accessed by other applications. 
LayTrax provides user authentication and authorization along with user-controllable tracking interval and distance options.

In order to access and view the stored data in maps and tables, a serverless Angular application based on the [Leapfire](https://github.com/lvhellgren/Leapfire)
application base is currently being developed.

#### Screens
1. Sign In
    - Email field
    - Password field
    - SIGN IN button
    - REQUEST SERVICE ACCESS button
2. Service Access Request
    - Administrator Email field
    - SEND button
    - RETURN button
3. Setup
    - Account field
    - MIN Footprint Separation field
    - GPS Time Interval field
    - Time Unit radio buttons (seconds or minutes)
    - START TRACKING button
    - SIGN OUT button
4. Running
    - STOP TRACKING button
    
#### Features

##### Cloud Database
LayTrax sends location data to a Firestore database, where needed authentication and authorization rules can be applied.

##### Background Worker
A LayTrax background worker performs all periodic processing activity making this work continue even when the application
is closed or after the user signs out from the phone session.

##### Sign In Requirement
LayTrax uses Firebase "Sign In with Email and Password" authentication for ensuring that only known users will have access to the database.

##### User and Account Management
LayTrax sends both user and account information to the database, thus, enabling required database rules to be applied to every request.

##### Settable Tracking Period
Users control the frequency of obtaining GPS location data. This time can be set from a few seconds to any number of minutes.

##### Settable Distance Between Database Entries
Users control the distance in meters between movements before entries are sent to the database.

#### License
MIT

#### Contact
lars@exelor.com