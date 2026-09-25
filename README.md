# Indoor Positioning System Wi-Fi Fingerprint using Q-WKNN Algorithm

This project is a thesis for my Bachelor's Degree (Sarjana Terapan Teknik) in Digital Telecommunication Network, Electrical Engineering Department, State Polytechnic of Malang (Politeknik Negeri Malang), 2026.

This study implements the **Q-Weighted K-Nearest Neighbor (Q-WKNN)** algorithm on an Android-based Indoor Positioning System using Wi-Fi Fingerprinting, with the conventional **WKNN** algorithm used as a performance comparison. The research was conducted on the 1st floor of AH Building, State Polytechnic of Malang.

This system works on the principle of utilizing Received Signal Strength (RSS) emitted by Wi-Fi Access Points to determine the position of the user indoors, where GPS signals are unreliable. In the design process, a fingerprinting database was built by collecting RSS data at 42 Reference Points (RP) and validating it against 19 Test Points (TP), covering 153 unique detected Access Points.

## Result

Based on the test results across 19 Test Points (190 test samples) with optimal parameters η = 4 and L = 70:

- **WKNN** achieved a mean position error of **3.72 m**, while **Q-WKNN** achieved a mean position error of **4.02 m** — WKNN slightly outperformed Q-WKNN in distance accuracy.
- For **room identification accuracy**, Q-WKNN reached **61.6%**, outperforming WKNN which achieved **48.9%**, particularly in lobby/hallway areas.
- Overall, no single algorithm was absolutely superior — performance depends on the environment characteristics and which metric is prioritized (distance accuracy vs. room identification accuracy).

The application also provides estimated distance and direction (azimuth) toward a selected destination room, following the corridor structure of the building.

## Documentation

*(Tambahkan screenshot aplikasi kamu di sini — misalnya tampilan denah/radio map Lantai 1 Gedung AH, halaman kalibrasi, hasil positioning, dan hasil analisis akurasi. Upload gambar ke repo atau ke sebuah image host, lalu sisipkan pakai format `![deskripsi](link-gambar)`.)*

Radio Map Lantai 1 Gedung AH:
![Radio Map Lantai 1] <img width="2084" height="985" alt="denah_ah" src="https://github.com/user-attachments/assets/cafcbc70-d127-4e2b-937c-af05445b010d" />


Description of project:

1. Home / Splash Menu
![Splash_Menu]
<img width="714" height="1599" alt="WhatsApp Image 2026-07-17 at 6 17 27 PM" src="https://github.com/user-attachments/assets/486b36e4-d1f4-4612-a5a6-2235f3b97970" />

![Home Menu] 
<img width="714" height="1599" alt="WhatsApp Image 2026-07-17 at 6 12 55 PM" src="https://github.com/user-attachments/assets/8e22386c-596c-4f4a-ab24-d215dd607e3a" />


3. Calibration (Fingerprinting)
![Calibration]
<img width="714" height="1599" alt="WhatsApp Image 2026-07-17 at 6 30 55 PM" src="https://github.com/user-attachments/assets/91de7db7-da3e-47df-b129-cad878041e2f" />


5. Result of Positioning
![Result of Positioning]
<img width="714" height="1599" alt="WhatsApp Image 2026-07-17 at 6 31 50 PM" src="https://github.com/user-attachments/assets/a5120fd0-3845-4b17-9af0-78fcdead5bca" />


7. Floor Map View 
![Floor Map View]
<img width="714" height="1599" alt="WhatsApp Image 2026-07-20 at 9 08 06 PM" src="https://github.com/user-attachments/assets/3d3ca520-3569-424c-93eb-67fad04659ba" />

9. Analysis / Accuracy Result
![Analysis Result]
<img width="714" height="1599" alt="WhatsApp Image 2026-07-17 at 6 32 46 PM" src="https://github.com/user-attachments/assets/4a77fa88-ff40-445a-a8d2-ec76c2c41842" />


## Built With

- **Language:** Java
- **Platform:** Android (min SDK 24, target SDK 35)
- **Build tool:** Gradle (Kotlin DSL)
- **IDE:** Android Studio

## How to Run

1. Clone this repository
   ```bash
   git clone https://github.com/Adfirdanii/Indoor-Positioning-System-Wi-Fi-Fingerprint-use-Algorithm-Q-WKNN.git
   ```
2. Open the project with **Android Studio**
3. Wait for Gradle sync to finish
4. Connect an Android device (or emulator with Wi-Fi scanning support)
5. Click **Run** ▶️

## References

1. R. Zhou, Y. Yang, and P. Chen, "An RSS Transform—Based WKNN for Indoor Positioning," *Sensors*, vol. 21, no. 17, p. 5685, 2021.
2. A. P. H. Yulianto, M. N. Zakaria, and A. W. Yulianto, "Indoor Positioning and Navigating System Application Using Wi-Fi with Fingerprinting Method and Weighted K-Nearest Neighbor Algorithm," *Jurnal Jaringan Telekomunikasi*, vol. 12, no. 3, 2022.

For further information, feel free to reach out:

email: adeachmdd@gmail.com
instagram: adfirdann
