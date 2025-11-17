# OrcaTestEngine  
### Deterministic Orchestration Framework for Android Application Stress & Reliability Testing

OrcaTestEngine is a deterministic, event-driven stress-testing framework designed to evaluate the reliability and edge-case behavior of Android applications running on ADB-connected devices or emulators. Every aspect of the test flow—events, retries, triggers, sequences, and reboot behavior—is defined in JSON, enabling reproducible runs and automation-friendly pipelines. Designed for maximum accessibility, the framework has zero external prerequisites and runs solely on a standard Android Studio environment, eliminating the need for complex setup or additional dependencies.


---

## 🚀 Key Features

### **Deterministic Execution**
- Event selection uses a seeded RNG  
- Engine tracks every RNG call  
- Failures can be reproduced exactly via `replay_state.json`

### **JSON-Driven Test Plans**
A single JSON file defines:
- Metadata  
- Events and sequences  
- Retry + cooldown strategies  
- Preconditions  
- Script execution (Shell, Python, Batch, PowerShell, etc.)  
- Metrics  
- Logcat configuration  
- State machine transitions  

### **Multi-Language Script Execution**
Built-in handlers:
- Shell  
- Python  
- Batch  
- PowerShell  
- Ruby  
- Node  
- Custom scripts

### **System Inspection Layer**
Pluggable architecture:
- Device online/offline detection  
- BOOT_COMPLETED detection  
- Process monitoring  
- Battery, network, charging state  
- File existence checks  
- ADB/root availability  
- App startup/restart  

### **Reboot-Aware Engine**
- Detects events that cause reboot  
- Handles offline → online → boot complete  
- Supports app relaunch  
- Supports logcat rotation

### **Detailed Summary Output**
Shows:
- Execution stats  
- Failures  
- Metrics  
- Final state  
- RNG seed + call count  

---



## 📁 Project Structure

```text
OrcaTestEngine/
 ├── build.gradle.kts
 ├── settings.gradle.kts
 ├── stress-config.json              # Example config
 ├── replay_state.json               # Saved RNG state
 ├── schemas/
 │    └── stress-test-config.schema.json
 └── src/
      ├── main/
      │    ├── kotlin/
      │    │      └── orca.engine/
      │    │             ├── config/            # Config loader + schema
      │    │             ├── core/              # OrcaEngine + runners + RNG
      │    │             ├── logging/           # Logcat + console logging
      │    │             └── system/            # SystemInspector implementations
      │    └── resources/
      └── test/
```

---


## 🧬 Architecture Overview

```text
+---------------------------+
|     stress-config.json    |
+---------------------------+
            |
            v
+---------------------------+
|     OrcaTestConfig        |
+---------------------------+
            |
            v
+--------------------------------------------------------+
|                     OrcaEngine                         |
|--------------------------------------------------------|
| Event selection | Retry | State machine | Reboot logic |
| Preconditions   | Script exec | Logcat | Metrics       |
+--------------------------------------------------------+
            |
            v
+---------------------------+
|      ScriptRunner         |
+---------------------------+
            |
            v
+--------------------------------------------+
|   ScriptHandlers (Shell, Python, Batch…)   |
+--------------------------------------------+
```


---

## ▶️ Running the Engine

### 1. Clone
```text
git clone https://github.com/waltcapers/OrcaTestEngine.git

```



### 2. Add config

Place your stress-config.json in the project root.

### 3. Run

```text
./gradlew run
```

### 4. Replay failures

engine.replay()

## Minimal Example Config

```text
{
  "randomSeed": 12345,
  "targetPackage": "com.example.app",
  "events": [
    {
      "id": "toggle_wifi",
      "type": "SCRIPT",
      "language": "SHELL",
      "script": { "inline": ["svc wifi disable", "sleep 1", "svc wifi enable"] },
      "weight": 5
    }
  ]
}
```


---



## License

OrcaTestEngine Commercial License v1.0
Copyright (c) 2025 Walt Capers

This license applies to all commercial users EXCEPT General Motors (GM).
General Motors is explicitly exempt and may use OrcaTestEngine under the
MIT License without commercial licensing requirements.

1. Permitted Use
   The Licensee (excluding GM) may integrate and distribute OrcaTestEngine
   as part of a proprietary or commercial product, service, or platform,
   provided a valid commercial license has been obtained.

2. Restrictions
   - OrcaTestEngine may not be sold or relicensed as a standalone product.
   - The Software may not be used to develop a competing stress-testing
     engine without explicit written consent.
   - Redistribution outside the Licensee’s organization is prohibited
     unless a commercial license explicitly permits it.
   - These restrictions do NOT apply to General Motors, which is exempt.

3. Modifications
   Licensees (excluding GM) may modify the Software for internal use.
   Modified versions may not be distributed commercially without a commercial
   distribution license.
   General Motors may modify and use internally under the MIT License.

4. Ownership
   OrcaTestEngine remains the intellectual property of the copyright holder.
   This license does not transfer ownership.

5. Warranty
   The Software is provided “as is,” without warranty unless otherwise
   specified in a separate commercial agreement.

For commercial licensing inquiries (excluding GM), contact:
waltercapers@gmail.com



