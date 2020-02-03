
LayTrax is an Android application for making a smart phone function as a GPS location tracking device. The application stores
location information in a cloud database, where both current and historical locations can be accessed by other applications. 
LayTrax provides user authentication and authorization along with user-controllable tracking interval and distance options.

A server-less Angular application [Tracker] (https://github.com/lvhellgren/Tracker) application is available for accessing
and defining geo-fencing conditions on the stored information.

#### Screens
1. Sign In
    - Email field
    - Password field
    - SIGN IN button
    - REQUEST SERVICE ACCESS button 
2. Service Access Request
    - Administrator Email field
    - SEND button for requesting that the particular device be given location write access to the
      database. If access is granted, the administrator will respond with the applicable account ID and user credentials.
    - RETURN button
3. Setup
    - Account field
    - MIN Location distance field
    - GPS Time Interval field
    - Time Unit radio buttons (seconds or minutes)
    - Accuracy radio buttons (High or Low)
    - START TRACKING button
    - SIGN OUT button
4. Running
    - STOP TRACKING button
    
#### Features

##### Cloud Database
LayTrax sends location data to user accounts in a Firestore database, where authentication and authorization rules are applied.

##### Android Service
A LayTrax service performs all periodic processing activity making this work continue even when the application
runs in the background.

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