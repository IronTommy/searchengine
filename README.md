# Search Engine - README
Привет! Добро пожаловать в Search Engine - 
поисковый движок, разработанный на базе Spring Boot.

## Описание проекта

Search Engine разрабатывается с использованием Spring Boot и MySQL 
для эффективного поиска по веб-страницам. На этом этапе добавляется система индексации, 
которая поддерживает лемматизацию и подсчет встречаемости слов.

## Стэк технологий

- Java
- Spring Boot
- MySQL
- HTML
- CSS
- Thymeleaf

## Установка

1. Клонируйте репозиторий:

   git clone https://github.com/IronTommy/searchengine.git
   cd search-engine

2. Запустите проект:

   ./mvnw spring-boot:run

   Проект будет доступен по адресу http://localhost:8080.

## Использование

### Индексация страницы

Для запуска обхода и индексации веб-страницы выполните следующий запрос:

POST http://localhost:8080/api/indexing/start
Content-Type: application/json

{
"url": "https://www.example.com"
}

##Остановка обхода

Чтобы остановить обход, выполните запрос:

POST http://localhost:8080/api/indexing/stop

## Поиск по запросу

Для выполнения поиска по запросу используйте следующий запрос:

GET http://localhost:8080/api/search
Content-Type: application/json

{
"query": "ваш_поисковый_запрос",
"site": "http://www.example.com"
}

#Ожидаемый результат

Результаты поиска представлены списком объектов со следующими полями:

uri — путь к странице вида /path/to/page/6784;
title — заголовок страницы;
snippet — фрагмент текста, в котором найдены совпадения;
relevance — релевантность страницы.


## Разработка

Проект разработан с использованием Java и Spring Boot. Для внесения изменений и улучшений:

1. Откройте проект в вашей любимой среде разработки.
2. Внесите необходимые изменения.
3. Запустите проект для проверки изменений.

## Развертывание базы данных

1. Создайте базу данных MySQL с именем search_engine:
   CREATE DATABASE search_engine;

2. Внесите настройки базы данных в файл src/main/resources/application.properties:
   spring.datasource.url=jdbc:mysql://localhost:3306/search_engine
   spring.datasource.username= ваш_пользователь
   spring.datasource.password= ваш_пароль
