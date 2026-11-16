BUILD THE APK
Upload this folder to GitHub, open the Actions tab, wait for "Build APK", download the Riya-APK artifact.
(Or open in Android Studio > Build > Build APK(s).)

FIRST RUN - tap the buttons in the app in order
1 Allow microphone, contacts, calls & notifications
2 Allow lock screen (Device admin)
3 Allow display over other apps
4 Allow background running (battery)  -> choose Allow
5 Allow Do Not Disturb access for Riya (this lets it mute the listening beep)
6 Enable Accessibility -> Riya -> On (only used to press Send in WhatsApp)
   (Android 13+: if it is greyed out, Settings > Apps > Riya > 3-dot menu > Allow restricted settings)
7 (Offline chat) In the app, 'Offline voice chat' card: tap DOWNLOAD (about 1.6 GB, Wi-Fi) once, or CHOOSE FILE if you already have the model file
8 Type the name (e.g. Janu) and tap START

COMMANDS (start with the name you set in the app, default Riya)
open <ANY app name>  (e.g. open Instagram, open Settings, open PhonePe)
lock screen | open camera
play <song> | play <song> on YouTube
call <name>   (if several match, Riya reads them numbered - just say the number, e.g. "three", no name needed)
send WhatsApp to <name> saying <message>
flashlight on / off
volume up / down / max / mute / 50
what's the time | what's the date
stop


NEW IN THIS VERSION
- Colourful screen with a live orb: Listening / Thinking / Speaking, plus what it heard and replied.
- Setup list shows a green tick for every step already done.
- Faster: one reused speech recogniser, instant restart, picks a contact the moment you say the number.
- Talks with you: after Riya speaks you can answer WITHOUT the name for 8 seconds.
- Small talk works offline (hello, how are you, jokes, battery, thanks, bye).
- Optional AI brain: paste an Anthropic API key in the app and Riya answers any question out loud.

OFFLINE VOICE CHAT
- Hearing: Vosk speech recognition, bundled in the APK (the GitHub build downloads the two speech models).
- Answers: Qwen2.5-1.5B running on the phone (MediaPipe). Say any question; no name needed (wake-word switch is OFF by default).
- Phone commands (open app, call, WhatsApp, flashlight...) still work the same way.

WORKS WITHOUT INTERNET: open apps, call, WhatsApp compose, flashlight, volume, time/date/battery, camera, lock screen,
close all apps, home/back/recents/notifications/screenshot, alarm and timer, wifi/bluetooth settings, local music, and chat (offline brain).
NEEDS INTERNET: YouTube/Spotify, sending the WhatsApp message itself, weather/news-type questions.

BRAINS: live questions (weather/news/score/price) use the online AI key when internet is on; everything else uses the on-phone brain, which also works offline.

SPEED: answers start speaking sentence-by-sentence while the model is still thinking; speech end is detected after about half a second. For the quickest answers use the FAST BRAIN (0.5B) button.

INCOMING CALLS: Riya reads out the caller's contact name, or the number digit by digit if it is not saved (works offline). Say 'answer' or 'reject'. Allow the Phone permissions when asked.

VOICE MATCH (only your voice)
- In the app, 'Voice Match' card: tap TRAIN MY VOICE and read the 5 sentences Riya says (quiet room, normal voice).
- After that Riya ignores other voices. If she rejects you, the screen shows the match score; train again in a quiet place. REMOVE deletes your voice.
- The GitHub build downloads the speaker model (vosk-model-spk-0.4) automatically.

TYPE BY VOICE
- Put the cursor in any text box (WhatsApp, mail, notes ...) and say: Riya, start typing. Then speak; Riya types your words with capitals, punctuation and spelling fixes.
- Say: send | new line | delete that | clear text | stop typing. Spoken punctuation works: comma, full stop, question mark.
- One sentence only: Riya, type I will be late today.
- Chats: 'open WhatsApp Ramu' or 'open WhatsApp Business Ramu' opens the chat; then speak your message and say 'send'.
- Needs Accessibility switched ON. For best accuracy use Google hearing (offline hearing OFF) and a Gemini key for smart auto-correct.

WHATSAPP CALLS
- 'Riya, open WhatsApp Business Ramu call'  -> normal WhatsApp voice call.
- 'Riya, open WhatsApp Ramu video call' / 'video call Ramu' / 'call Ramu on WhatsApp' -> video or voice call in WhatsApp or WhatsApp Business.
- Works best for contacts that are on WhatsApp (they have a call shortcut). Otherwise Riya opens the chat and taps the call button (needs Accessibility ON).

SEVERAL PEOPLE WITH THE SAME NAME
- 'Riya, open WhatsApp Rama video call' with contacts Rama, Ramya Krishna and Rama Reddy: Riya reads them numbered ('1, Rama. 2, Ramya Krishna. 3, Rama Reddy. Say the number to start the video call'). Say 'two' (or the name) and she does it. Say 'cancel' to stop.
- Names that sound alike (Rama / Ramya / Ramu) are all offered. Works for phone calls, WhatsApp / WhatsApp Business chats, voice calls, video calls and messages.
