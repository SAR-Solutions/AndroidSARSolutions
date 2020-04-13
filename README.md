<p align="center">
  <img src="app/src/main/res/mipmap-xhdpi/app_icon.png" width="25%">
</p>

# SAR Coordinator
The SAR Coordinator Application aids volunteers in search and rescue operations by leveraging the phone's GPS to track volunteer location, and enabling volunteers to send shift reports to the county admins.

Built by SAR Solutions
	
## Design Iterations

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

## External Links
- [Play Store Link](https://play.google.com/store/apps/details?id=com.sarcoordinator.sarsolutions)
- [Landing Page](https://sarcoordinator.com/)
- [Admin Page](https://sarcoordinator.com/login)
-	[Privacy Policy](https://sar-coordinator.flycricket.io/privacy.html)
