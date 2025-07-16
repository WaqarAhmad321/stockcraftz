# Welcome to StockCraftz!
StockCraftz is a JavaFX-based Minecraft insipired gamified inventory management application designed for managing, crafting and trading virtual items. Users can register, manage raw materials and crafted items, sell crafted items on a marketplace. It has gamification features like Leaderboard, Dashboard and recent activities. The application leverages Postgresql database backend using a custom made Java library by myself and follows Object-Oriented Programming (OOP) principles for a robust and extensible design.

## Table of Contents
- [Features](#features)
- [Screenshots](#screenshots)
- [Installation](#installation)
- [Usage](#usage)
- [Project Structure](#project-structure)
- [Technologies Used](#technologies-used)
- [Contributing](#contributing)
- [Contact](#contact)

## Features
- User registration and login with password hashing (BCrypt).
- Management of raw materials (e.g., APPLE, DIAMOND, GOLD) with initial quantities upon registration.
- Crafting item inventory with quantity tracking and removal functionality.
- Marketplace integration to sell crafted items.
- Password reset functionality with old password verification.
- Responsive JavaFX UI with pagination and search filters.
- Database-driven operations using asynchronous queries.

## Screenshots
- **Login Page**: ![Register Page](https://github.com/user-attachments/assets/b14a977b-c6fc-4ee6-b8cc-b84cb41418d7)
- **Register Page**: ![Register Page](https://github.com/user-attachments/assets/94b208c1-ef69-4276-b715-0f9173330fe4)
- **Menu Page**: ![Menu Page](https://github.com/user-attachments/assets/d6cc059c-7813-4b7e-84bd-1f6c394ddeb6)
- **Dashboard Page**: ![Dashboard Page](https://github.com/user-attachments/assets/3880b26b-be6a-4e4e-9854-ac2e2db219c4)
- **Raw Materials Page**: ![Raw Materials Page](https://github.com/user-attachments/assets/17e28b43-4867-4ad8-b648-c4edbddc37da)
- **Crafting Page**: ![Crafting Page](https://github.com/user-attachments/assets/d6d9cdd2-c4ec-4ac0-8ab4-05bc6333eb14)
- **Crafted Items Page**: ![Crafted Items Page](https://github.com/user-attachments/assets/4d47daa2-6832-48d3-a826-fc920de806d9)
- **Leaderboard Page**: ![Crafted Items Page](https://github.com/user-attachments/assets/a286f62b-8906-4c14-86cd-1c5acd200db2)
- **Marketplace Page**: ![Marketplace Page](https://github.com/user-attachments/assets/188e5b4d-8fea-470f-bf1e-13625bf846a0)
- **Reset Password Page**: ![Reset Password Page](https://github.com/user-attachments/assets/5c2ccfe2-6f50-47e0-9f04-5eb2d02c6f1c)

## Installation

### Prerequisites
- Java Development Kit (JDK) 17 or higher.
- 
### Steps
1. **Clone the Repository**
   ```bash
   git clone https://github.com/WaqarAhmad321/stockcraftz.git
   cd stockcraftz
   ```

2. **Build the Project**
   ```bash
   mvn clean install
   ```

3. **Run the Application**
   ```bash
   mvn javafx:run
   ```

## Usage
- **Register**: Navigate to the register page, enter a username, password, and confirm password to create an account. Initial raw materials are added automatically.
- **Login**: Use the login page to access the menu.
- **Manage Raw Materials**: View and manage raw materials with search and pagination.
- **Manage Crafted Items**: View, sell, or remove crafted items with quantity tracking.
- **Reset Password**: Use the reset password page to update your password with the old password verification.
- **Navigate**: Use the navbar to switch between pages (e.g., crafting, marketplace).

## Project Structure
```
stockcraftz/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com.bigsteppers.stockcraftz/
│   │   │       ├── controllers/          # Controller classes (e.g., RegisterController, RawMaterialsController)
│   │   │       ├── model/               # Model classes (e.g., User, RawMaterial, CraftedItem)
│   │   │       ├── utils/               # Utility classes (e.g., FXUtils, DBUtils)
│   │   │       └── interfaces/          # Interfaces (e.g., LoadablePage)
│   │   └── resources/
│   │       └── com.bigsteppers.stockcraftz/
│   │           ├── views/              # FXML files (e.g., register.fxml, raw_materials.fxml)
│   │           ├── images/             # Image assets (e.g., material icons)
│   │           └── styles.css          # CSS for styling
├── pom.xml                             # Maven build file
└── README.md                           # This file
```

## Technologies Used
- **Language**: Java 17
- **Framework**: JavaFX
- **Styling**: JavaFX CSS
- **Build Tool**: Maven
- **Database**: PostgreSQL (or compatible)
- **Security**: BCrypt for password hashing
- **OOP Concepts**: Encapsulation, Abstraction, Inheritance, limited Polymorphism

## Contributing
I welcome contributions to enhance StockCraftz! Here's how you can help:
1. Fork the repository.
2. Create a new branch (`git checkout -b feature/your-feature`).
3. Make your changes and commit them (`git commit -m "Add your message"`).
4. Push to the branch (`git push origin feature/your-feature`).
5. Open a Pull Request with a description of your changes.

Please ensure your code follows the existing style.

## Contact
- **Author**: Waqar Ahmad
- **GitHub**: [https://github.com/WaqarAhmad321](https://github.com/WaqarAhmad321)
