# NovaCut Studio 🎬

> **The Next-Gen All-in-One Video & Photo Editing Engine**  
> Copyright © 2026 Master H. All Rights Reserved.

---

## 🚀 Overview

**NovaCut Studio** is a high-performance Android video and photo editing application built with **Jetpack Compose**, **Material 3**, and modern multimedia processing pipelines. It delivers CapCut-grade creative power with an ultra-responsive dark-theme timeline UI.

---

## ✨ Features

### 1. Multi-Track Timeline & Non-Destructive Editing
* **Multi-Track Engine:** Main video track, Picture-in-Picture (PIP) overlay layers, dedicated soundtrack/audio layers, and text/auto-captions tracks.
* **Precision Scrubbing:** Interactive drag-to-seek needle with real-time timecode ruler.
* **Non-Destructive Trimming:** Instant split, start/end trim, clip reordering, duplicate, and delete without altering original media files.

### 2. CapCut-Grade Tools Suite
* **Speed Ramping:** Smooth speed control from 0.2x slow-mo to 4.0x fast-motion.
* **AI Smooth Slow-Motion:** Optical flow frame interpolation simulation for stutter-free slow-mo clips.
* **PIP & Overlays:** Multi-layer video and photo compositing with custom blending modes.
* **Cinematic Filters & Shaders:** Original, Cinematic Teal/Orange, Cyberpunk Neon, Vintage 90s, Warm Gold, Film Noir, and Emerald Pop presets.
* **Color Grading (Adjust):** Fine-grained sliders for Brightness, Contrast, Saturation, Exposure, and Temperature.
* **Audio Studio:** Audio track mixing, original audio mute, and voice changer FX (Deep, Cyborg, Chipmunk, Echo).
* **AI Auto-Captions:** Automated speech-to-text subtitle generation with synchronized timing and typewriter animation.
* **AI Cutout & Chroma Key:** Green screen background removal and semantic body matting toggles.
* **Dynamic Canvas:** Seamlessly switch aspect ratios between **9:16 (Reels/Shorts)**, **16:9 (YouTube)**, **1:1 (Square)**, and **4:5 (Feed)**.

### 3. Hardware-Accelerated 4K Export
* Selectable resolutions: **720p HD**, **1080p Full HD**, and **4K Ultra HD**.
* Frame rates: **24 FPS**, **30 FPS**, and **60 FPS**.
* Direct automatic export to the device's public **`/sdcard/Download/`** folder for instant viewing and sharing.

---

## 🛠️ Tech Stack & Architecture

* **UI Layer:** Jetpack Compose + Material 3 (Dark Slate & Cyan/Magenta neon theme)
* **Architecture:** MVVM + StateFlow reactive unidirectional data flow
* **Multimedia Engine:** Android Media3 / ExoPlayer & MediaCodec hardware acceleration
* **Build System:** Gradle 8.4 with Android Gradle Plugin 8.3.1 (Kotlin 1.9.22)
* **Automation:** Universal Android APK Automation workflow via GitHub Actions

---

## 📦 Automated APK Generation

The repository includes `.github/workflows/android-apk-build.yml` which automatically:
1. Triggers on every push and tag.
2. Configures Java JDK 17 and Gradle.
3. Compiles the Android Debug & Release APK.
4. Uploads downloadable artifacts and publishes versioned GitHub releases.

---

## 📄 License & Legal Notice

Copyright © 2026 **Master H**. All Rights Reserved.  
Proprietary & Confidential. Unauthorized distribution, copying, or reverse engineering is strictly prohibited.
