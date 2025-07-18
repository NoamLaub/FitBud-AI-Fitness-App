# 🏋️‍♂️ FitBud - AI Fitness Companion

<div align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green.svg" alt="Platform">
  <img src="https://img.shields.io/badge/Language-Kotlin-blue.svg" alt="Language">
  <img src="https://img.shields.io/badge/AI-OpenAI%20GPT--3.5-orange.svg" alt="AI">
  <img src="https://img.shields.io/badge/Database-Firebase-yellow.svg" alt="Database">
  <img src="https://img.shields.io/badge/License-MIT-brightgreen.svg" alt="License">
</div>

## 📱 About FitBud

**FitBud** is your intelligent fitness companion that combines the power of AI with personalized health tracking. Built with cutting-edge technology, FitBud provides personalized meal suggestions, weight tracking, expert guidance, and motivational support to help you achieve your fitness goals.

### 🌟 Key Features

- **🤖 AI-Powered Chatbot**: Intelligent conversations powered by OpenAI GPT-3.5 Turbo
- **📊 Weight Tracking**: Monitor your progress with detailed weight logs and trend analysis
- **🍽️ Smart Meal Suggestions**: Personalized meal recommendations based on your profile and goals
- **⏰ Intelligent Reminders**: Automated meal and weigh-in notifications
- **👨‍⚕️ Expert System**: Professional oversight with message review and editing capabilities
- **💬 WhatsApp-Style Chat**: Intuitive messaging interface with sound effects
- **🔐 Secure Authentication**: Firebase-powered user authentication and data protection

## 🚀 Getting Started

### Prerequisites

- **Android Studio** Arctic Fox or later
- **Android SDK** API level 24 or higher
- **Java 11** or newer
- **Firebase Account** for backend services
- **OpenAI API Key** for chatbot functionality

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/NoamLaub/FitBud-AI-Fitness-App.git
   cd FitBud-AI-Fitness-App
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Select "Open an existing project"
   - Navigate to the cloned directory

3. **Configure Firebase**
   - Add your `google-services.json` file to the `app/` directory
   - Ensure Firebase Authentication and Realtime Database are enabled

4. **Set up OpenAI API**
   - Open `app/src/main/java/com/example/finalproject/OpenAIApi.kt`
   - Replace `YOUR_OPENAI_API_KEY_HERE` with your actual OpenAI API key
   - **⚠️ Important**: Never commit your actual API key to version control

5. **Build and Run**
   ```bash
   ./gradlew assembleDebug
   ```

## 🏗️ Architecture

### Tech Stack

- **Frontend**: Android (Kotlin)
- **Backend**: Firebase Realtime Database
- **Authentication**: Firebase Auth
- **AI Integration**: OpenAI GPT-3.5 Turbo API
- **HTTP Client**: OkHttp3
- **Notifications**: Android AlarmManager

### Project Structure

```
app/
├── src/main/java/com/example/finalproject/
│   ├── MainActivity.kt              # Main chat interface
│   ├── LoginActivity.kt             # User authentication
│   ├── ProfileSetupActivity.kt      # User profile management
│   ├── ExpertDashboardActivity.kt   # Expert user interface
│   ├── ExpertUserChatActivity.kt    # Expert chat management
│   ├── OpenAIApi.kt                 # AI integration
│   ├── ReminderReceiver.kt          # Notification handling
│   └── models/
│       ├── User.kt                  # User data model
│       ├── Expert.kt                # Expert data model
│       └── ChatMessage.kt           # Message data model
├── src/main/res/
│   ├── layout/                      # UI layouts
│   ├── values/                      # Colors, strings, themes
│   └── raw/                         # Sound effects
└── google-services.json             # Firebase configuration
```

## 🎯 Features Deep Dive

### 🤖 AI Chatbot
- **Contextual Conversations**: Maintains chat history for personalized interactions
- **Profile-Aware Responses**: Considers user's age, weight, goals, and preferences
- **Meal Suggestions**: Automatically provides meal recommendations before meal times
- **Progress Analysis**: Analyzes weight trends and provides motivational feedback

### 📊 Weight Tracking
- **Smart Logging**: Prevents duplicate entries on the same day
- **Trend Analysis**: AI-powered insights into weight progress
- **Weigh-in Reminders**: Customizable reminder schedule
- **Visual Progress**: Clear tracking of weight changes over time

### 🍽️ Meal Management
- **Personalized Suggestions**: Tailored to dietary preferences and caloric goals
- **Timed Reminders**: Notifications before each scheduled meal
- **Nutritional Awareness**: Considers protein goals and dietary restrictions
- **Contextual Recommendations**: Adapts to time of day and meal type

### 👨‍⚕️ Expert System
- **Professional Oversight**: Experts can review and edit AI responses
- **User Assignment**: Balanced distribution of users among experts
- **Report System**: Users can flag inappropriate or unhelpful responses
- **Quality Assurance**: Ensures high-quality, safe health advice

### 💬 Chat Interface
- **WhatsApp-Style Design**: Familiar and intuitive messaging experience
- **Message Bubbles**: Visual distinction between user and AI messages
- **Sound Effects**: Audio feedback for sent and received messages
- **Auto-Scroll**: Always shows the latest messages
- **Keyboard Adaptation**: Smart UI adjustments when typing

## 🔧 Configuration

### Firebase Setup
1. Create a new Firebase project
2. Enable Authentication (Email/Password)
3. Create a Realtime Database
4. Add Android app to Firebase project
5. Download and add `google-services.json`

### OpenAI API Setup
1. Create an OpenAI account
2. Generate an API key
3. Replace the placeholder in `OpenAIApi.kt`
4. Ensure you have sufficient API credits

### Notification Permissions
The app requests notification permissions for:
- Meal reminders
- Weigh-in alerts
- Expert notifications

## 🎨 UI/UX Design

### Color Palette
- **Primary Green**: `#7BC47F` - Represents health and vitality
- **Light Green**: `#E9F5E1` - Soft, calming background
- **Accent Blue**: `#4A90E2` - User message bubbles
- **Text Colors**: `#222222` (primary), `#888888` (hints)

### Typography
- **Font Family**: `sans-serif-medium` for consistency
- **Accessibility**: High contrast ratios for readability
- **Responsive**: Adapts to different screen sizes

## 📱 User Experience

### Onboarding Flow
1. **Welcome Screen**: Introduction to FitBud
2. **Registration**: Secure account creation
3. **Profile Setup**: Personalized fitness profile
4. **AI Introduction**: Personalized welcome message
5. **First Interaction**: Guided chat experience

### Daily Workflow
1. **Morning Weigh-in**: Optional weight logging
2. **Meal Reminders**: Timely nutrition suggestions
3. **Chat Interactions**: Ongoing support and guidance
4. **Progress Tracking**: Regular check-ins and analysis

## 🔒 Security & Privacy

### Data Protection
- **Firebase Security Rules**: Strict access controls
- **User Data Isolation**: Each user's data is private
- **No Password Storage**: Passwords handled by Firebase Auth
- **API Key Security**: Environment-based configuration

### Privacy Features
- **Minimal Data Collection**: Only fitness-relevant information
- **User Control**: Users can edit their profiles
- **Expert Oversight**: Professional review of AI responses
- **Secure Communication**: Encrypted data transmission

## 🧪 Testing

### Manual Testing Checklist
- [ ] User registration and login
- [ ] Profile creation and editing
- [ ] Weight logging functionality
- [ ] Meal reminder notifications
- [ ] Chat message sending/receiving
- [ ] Expert dashboard access
- [ ] Message reporting system

### Test Scenarios
- **New User Journey**: Complete onboarding flow
- **Returning User**: Login and data persistence
- **Expert Workflow**: Dashboard and message editing
- **Notification System**: Reminder delivery and timing

## 🚀 Deployment

### Release Build
```bash
./gradlew assembleRelease
```

### APK Generation
```bash
./gradlew bundleRelease
```

### Play Store Preparation
1. Generate signed APK
2. Create store listing
3. Add screenshots and descriptions
4. Configure app permissions
5. Submit for review

## 🤝 Contributing

We welcome contributions to FitBud! Here's how you can help:

### Development Setup
1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

### Code Style
- Follow Kotlin coding conventions
- Use meaningful variable names
- Add comments for complex logic
- Maintain consistent formatting

### Areas for Contribution
- **UI/UX Improvements**: Enhanced visual design
- **Feature Additions**: New functionality
- **Performance Optimization**: Faster loading times
- **Bug Fixes**: Issue resolution
- **Documentation**: Improved guides and comments

## 📋 Roadmap

### Version 2.0 (Planned)
- [ ] **Exercise Tracking**: Workout logging and suggestions
- [ ] **Social Features**: Friend connections and challenges
- [ ] **Advanced Analytics**: Detailed progress reports
- [ ] **Wearable Integration**: Smartwatch compatibility
- [ ] **Offline Mode**: Basic functionality without internet

### Version 2.1 (Future)
- [ ] **Nutrition Database**: Comprehensive food tracking
- [ ] **Recipe Suggestions**: Personalized meal plans
- [ ] **Video Workouts**: Integrated exercise videos
- [ ] **Health Metrics**: Heart rate, sleep tracking
- [ ] **Multi-language Support**: International accessibility

## 🐛 Known Issues

### Current Limitations
- **API Dependency**: Requires internet for AI features
- **Notification Reliability**: May vary by device manufacturer
- **Battery Optimization**: Some devices may limit background notifications

### Workarounds
- **Offline Fallback**: Basic functionality available without AI
- **Manual Refresh**: Pull-to-refresh for data updates
- **Battery Settings**: Guide users to whitelist the app

## 📞 Support

### Getting Help
- **GitHub Issues**: Report bugs and request features
- **Documentation**: Comprehensive guides and API references
- **Community**: Join discussions and share experiences

### Contact Information
- **Developer**: Noam Laub
- **Email**: [Your Email]
- **GitHub**: [@NoamLaub](https://github.com/NoamLaub)

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

### Special Thanks
- **OpenAI**: For providing the GPT-3.5 Turbo API
- **Firebase**: For robust backend infrastructure
- **Android Community**: For extensive documentation and support
- **Beta Testers**: For valuable feedback and bug reports

### Third-Party Libraries
- **OkHttp3**: HTTP client for API communication
- **Firebase SDK**: Authentication and database services
- **Android Jetpack**: Modern Android development components

---

<div align="center">
  <h3>🌟 Made with ❤️ for a healthier world 🌟</h3>
  <p>FitBud - Your AI-powered journey to better health starts here!</p>
</div>

---

**⚡ Ready to get fit? Download FitBud and start your transformation today!**
