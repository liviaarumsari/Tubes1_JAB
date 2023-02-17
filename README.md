# JAB
Tugas Besar 1 IF2211 Strategi Algoritma
<br />
Pemanfaatan Algoritma Greedy dalam Aplikasi Permainan “Galaxio”

## Table of Contents
* [General Info](#general-information)
* [Dependencies](#dependencies)
* [Build](#build)
* [Tech Stack](#tech-stack)
* [Project Structure](#project-structure)
* [Credits](#credits)

## General Information
Algoritma _Greedy_ yang kami implementasikan ke dalam _bot_ ini meliputi:
* Strategi _**torpedo defense**_. Setiap tick game akan dilakukan pengecekan apakah ada _torpedo salvo_ yang mengarah ke _bot_. Hal ini diprioritaskan karena _torpedo salvo_ dapat ditembakkan dalam jumlah banyak (lebih dari 1) sehingga berpotensi untuk lansung mengeliminasi _bot_ dari permainan.
* Strategi _**attack**_. Apabila terdapat ships yang berdekatan dengan _bot_, dengan ukuran yang lebih kecil maka _bot_ akan menembakkan _torpedo salvo_, dan jika jaraknya semakin dekat, maka _bot_ akan mengganti _action_ menjadi _STARTAFTERBURNER_ dan mengejar _ship_ tersebut. Jika tersisa 1 _ship_ lawan dan _bot_ memiliki _size_ yang lebih besar, maka _bot_ akan mengaktifkan _aggressive attacking_ pada _attack_, yang membuat _bot_ menembakkan proyektil _teleporter_ ke arah _ship_ lawan dan akan melakukan _teleport_ jika proyektil berhasil mendekati _ship_ lawan.
* Strategi menghindari _ship_ yang berukuran yang lebih besar. Jika masih dalam jarak yang tidak terlalu dekat, _bot_ akan mengubah _heading_-nya untuk menjauh dari _ship_ tersebut. Jika _ship_ semakin dekat, maka _bot_ akan menembakkan _torpedo salvo_ ke arah _ship_ tersebut (_exchange size_).
* Strategi menghindari objek - objek berbahaya (_gas clouds_ dan _asteroid fields_). _Bot_ akan lansung mengubah _heading_-nya menuju arah yang tidak terdapat objek-objek berbahaya.
* Strategi _**farming**_. _Bot_ akan melakukan _farming_ yaitu mencari makan dengan _superfood_ sebagai prioritas.
* Strategi _**move to center**_. _Bot_ akan lansung mengubah _heading_-nya menuju pusat _map_ dan bergerak ke arah pusat _map_ agar tetap dalam batasan _boundary_.

## Dependencies
1. Apache Maven ^3.8.7 
2. .NET Core 3.1

## Build
from this project root,
```shell
mvn clean package
```
Once the build is finished, a target folder will be created at the project root with `JAB.jar` inside it. You can place the `JAB.jar` file in your target folder.

## Tech Stack
* OpenJDK 18.0.2.1

## Project Structure
```bash
.
│   README.md
│   .gitignore
│
├───src
│   └───main
│       └───java
│           ├───Enums
│           ├───Models
│           └───Services
├───doc
│
├───Dockerfile
│
├───pom.xml
│
└───target
```

## Credits
This project is implemented by:
1. Bill Clinton (13521064)
2. Angela Livia Arumsari (13521094)
3. Jimly Firdaus (13521102)
