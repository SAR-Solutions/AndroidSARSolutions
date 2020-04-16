<p align="center">
  <img src="app/src/main/res/mipmap-xhdpi/app_icon.png" width="25%">
</p>

# SAR Coordinator
The SAR Coordinator Application aids volunteers in search and rescue operations by leveraging the phone's GPS to track volunteer location, and enabling volunteers to send shift reports to the county admins.

Built by SAR Solutions

<a href="https://play.google.com/store/apps/details?id=com.sarcoordinator.sarsolutions"><img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" width="25%"></a>

## Build Instructions
- Open project in [Android Studio](https://developer.android.com/studio/index.html)
  - Android Studio version needs to be greater than or equal to 4.0
- Create a new project on [Firebase](https://firebase.google.com/)
  - Go to 'Project settings' in the firebase project and under 'Your apps' click on the Android Icon
  - Enter `com.sarsolutions.sarcoordinator` in the 'Android package name' field
  - Continue through the process by following instructions
    - Make sure to download the config file when prompted
  - Place `google-services.json` (the downloaded config file) in the `app` directory
- Create a new project on [Google Cloud](https://cloud.google.com/)
	-  Enable the `Maps SDK for Android` under 'APIs & Services'
	- Create an API key by clicking on 'Create  Credentials' > API key on the 'APIs & Services' page
	- Follow the steps to create the API key
- Open the build environment's `gradle.properties` file
	- On Windows: `C:\Users\<you>\.gradle\gradle.properties`
  - On Mac/Linux: `/Users/<you>/.gradle/gradle.properties`
	- Enter the Google maps API key from the previous step in the following format
		- `GOOGLE_MAPS_API_KEY="<YOUR_API_KEY_HERE>"`
			- Replace everything inbetween the brackets (including them) with your API key
- In Android Studio, go to `Build > Rebuild Project`
	- If any errors occur, go to `File > Invalidate Caches / Restart > Invalidate and Restart` and try again

## Design Iterations
This section shows the UI design changes and reasons behind them.All of the changes aren't shown, only the major ones.

### HackTheU
The first functional componenet of the app was the tracking feature which was made for HackTheU where my team won first place.
<p align="center">
	<img hspace="20" src="static/HackTheU/HackTheU%201.png" width="25%">
	<img hspace="20" src="static/HackTheU/HackTheU%202.png" width="25%">
</p>

Not a lot to see here design-wise as most of the work happened in the backend.

### Mockup
These are the mockups that were made after the hackathon and before any progress was made.
<p align="center">
	<img hspace="20" src="static/Mockups/Mockup%20-%20Login.png" width="25%">
	<img hspace="20" src="static/Mockups/Mockup%20-%20Cases%20List.png" width="25%">
	<img hspace="20" src="static/Mockups/Mockup%20-%20Tracking.png" width="25%">
</p>

<p align="center">
	<img hspace="20" src="static/Mockups/Mockup%20-%20Shift%20Report.png" width="25%">
</p>

A lot of the componenets aren't following material guidelines, the cases list isn't user friendly, the buttons are placed poorly, the navbar has no depth and there isn't a lot of depth in the UI overall (except for the cards, which are ready for take off).

With my limited knowledge about design and user interaction at the time, these were nothing more than glorified wireframes.

### Alpha (12:23:19)
The inital iteration of the app and the first realiztion of the mockups with a few design improvements. There were quit a few additions comapred to the mockups as I hadn't gone over the minute details while designing the mockups.

#### Login Screen
<p align="center">
	<img hspace="20" src="static/Alpha%20(12:23:19)/Alpha%20-%20Login.png" width="25%">
</p>

I reduced the icon size, changed the text boxes to be outlined and rounded the "Sign In" button in order to better follow material guidelines. I also added a "forgot password" button which had been overlooked when making the mockups. 

#### Reset Password Screen
<p align="center">
	<img hspace="20" src="static/Alpha%20(12:23:19)/Alpha%20-%20Reset%20Password.png" width="25%">
</p>

This is more or less the same as the login screen.

#### Cases List Screen
<p align="center">
	<img hspace="20" src="static/Alpha%20(12:23:19)/Alpha%20-%20Case%20List.png" width="25%">
</p>

The toolbar is the same color as the theme surface and elevated in order to be less distracting, and the list items are cards in order to make them easier to navigate and read. However, the cards themselves are displeasing to look at, specially with the contrast between them and the text on them.

#### Tracking Screen
<p align="center">
	<img hspace="20" src="static/Alpha%20(12:23:19)/Alpha%20-%20Tracking.png" width="25%">
	<img hspace="20" src="static/Alpha%20(12:23:19)/Alpha%20-%20Tracking%20–%201.png" width="25%">
</p>

The tracking screen is the same as from the hackathon version except for the addition of the toolbar, new material button style and button colors to better indicate the status of the shift.

#### Settings Screen
<p align="center">
	<img hspace="20" src="/static/Alpha%20(12:23:19)/Alpha%20-%20Settings.png" width="25%">
</p>

Another screen that had been overlooked in the mockups was the settings screen. The settings screen only had the options to select the theme and enable/disable testing mode. The settings screen could be accessed from the droop down menu along with the signout button.

## External Links
- [Play Store Link](https://play.google.com/store/apps/details?id=com.sarcoordinator.sarsolutions)
- [Landing Page](https://sarcoordinator.com/)
- [Admin Page](https://sarcoordinator.com/login)
- [Privacy Policy](https://sar-coordinator.flycricket.io/privacy.html)
