import os
import json
from pathlib import Path

def get_data_file(activity):
    return Path(activity.getFilesDir()) / "progress.json"

def load_progress(activity):
    data_file = get_data_file(activity)
    if data_file.exists():
        with open(data_file, 'r') as f:
            return json.load(f)
    return {
        'current_week': "Неделя 1 — База Python",
        'current_day': 1,
        'completed_days': {}
    }

def save_progress(activity, data):
    data_file = get_data_file(activity)
    with open(data_file, 'w') as f:
        json.dump(data, f)

DAYS_DATA = {
    "Неделя 1 — База Python": {
        1: {
            "title": "День 1: Введение в Python",
            "theory": "Установка Python, интерпретатор, print(), переменные, типы данных (int, float, str, bool)",
            "practice": "Калькулятор двух чисел",
            "tasks": "Настроить рабочее окружение, написать простой калькулятор"
        },
        2: {
            "title": "День 2: Ввод и вывод данных",
            "theory": "input(), преобразование типов, арифметика, f-строки",
            "practice": "Анкета пользователя",
            "tasks": "Создать программу с вводом имени, возраста, города"
        },
        3: {
            "title": "День 3: Условные операторы",
            "theory": "if, elif, else, логические операторы (and, or, not)",
            "practice": "Проверка возраста",
            "tasks": "Написать программу категоризации по возрасту"
        },
        4: {
            "title": "День 4: Циклы",
            "theory": "while, for, range(), break/continue",
            "practice": "Таблица умножения, Угадай число",
            "tasks": "Создать игру угадай число с подсказками"
        },
        5: {
            "title": "День 5: Списки",
            "theory": "list, индексы, срезы, методы списков",
            "practice": "Список покупок",
            "tasks": "Программа добавления/удаления товаров"
        },
        6: {
            "title": "День 6-7: Консольная игра",
            "theory": "Повторение пройденного",
            "practice": "Камень-ножницы-бумага",
            "tasks": "Создать игру против компьютера"
        }
    },
    "Неделя 2 — Структуры данных и функции": {
        1: {
            "title": "День 1: Кортежи, множества, словари",
            "theory": "tuple, set, dict",
            "practice": "Телефонная книга",
            "tasks": "Создать словарь контактов с поиском"
        },
        2: {
            "title": "День 2: Функции",
            "theory": "def, return, аргументы",
            "practice": "Калькулятор через функции",
            "tasks": "Создать функции для базовых операций"
        },
        3: {
            "title": "День 3: Область видимости и рекурсия",
            "theory": "Глобальные/локальные переменсия",
            "ные, рекурpractice": "Рекурсивная функция",
            "tasks": "Вычислить факториал рекурсивно"
        },
        4: {
            "title": "День 4: Работа со строками",
            "theory": "Методы строк (split, join, upper, lower...)",
            "practice": "Анализатор текста",
            "tasks": "Подсчитать слова, символы в тексте"
        },
        5: {
            "title": "День 5: Работа с файлами",
            "theory": "open, read, write",
            "practice": "Список дел в файл",
            "tasks": "Сохранять/загружать задачи из файла"
        },
        6: {
            "title": "День 6-7: To-Do лист",
            "theory": "Повторение",
            "practice": "To-Do с файловым хранилищем",
            "tasks": "Полноценное приложение списка дел"
        }
    },
    "Неделя 3 — ООП": {
        1: {
            "title": "День 1: Классы и объекты",
            "theory": "class, объекты, __init__",
            "practice": "Создание класса",
            "tasks": "Создать класс Человек с атрибутами"
        },
        2: {
            "title": "День 2: Методы и атрибуты",
            "theory": "self, методы экземпляра",
            "practice": "Расширение класса",
            "tasks": "Добавить методы в класс Человек"
        },
        3: {
            "title": "День 3: Наследование",
            "theory": "Наследование, инкапсуляция",
            "practice": "Иерархия классов",
            "tasks": "Создать класс Студент -> Человек"
        },
        4: {
            "title": "День 4: Магические методы",
            "theory": "__str__, __repr__",
            "practice": "Отладка класса",
            "tasks": "Добавить магические методы"
        },
        5: {
            "title": "День 5: Банковский аккаунт",
            "theory": "ООП на практике",
            "practice": "BankAccount",
            "tasks": "Класс с депозитом, снятием, балансом"
        },
        6: {
            "title": "День 6-7: Улучшение проекта",
            "theory": "Повторение ООП",
            "practice": "Доработка BankAccount",
            "tasks": "Добавить историю операций"
        }
    },
    "Неделя 4 — Практика и алгоритмы": {
        1: {
            "title": "День 1: Сортировки",
            "theory": "Пузырьковая, быстрая сортировка",
            "practice": "Сортировка массива",
            "tasks": "Реализовать 2 вида сортировки"
        },
        2: {
            "title": "День 2: Поиск",
            "theory": "Линейный, бинарный поиск",
            "practice": "Поиск в массиве",
            "tasks": "Реализовать бинарный поиск"
        },
        3: {
            "title": "День 3-7: Решение задач",
            "theory": "Практика на Codewars/LeetCode",
            "practice": "5-10 задач в день",
            "tasks": "Решать задачи ежедневно"
        }
    },
    "Неделя 5 — Библиотеки": {
        1: {
            "title": "День 1: pip и установка",
            "theory": "pip, установка библиотек",
            "practice": "Установка пакетов",
            "tasks": "Установить requests, random, datetime"
        },
        2: {
            "title": "День 2: requests и API",
            "theory": "Работа с API, HTTP запросы",
            "practice": "Курс валют",
            "tasks": "Получить курс USD/RUB через API"
        },
        3: {
            "title": "День 3-7: Проект",
            "theory": "datetime, random",
            "practice": "Улучшение проекта",
            "tasks": "Добавить больше функций"
        }
    },
    "Неделя 6 — GUI или Telegram-бот": {
        1: {
            "title": "День 1-2: Выбор направления",
            "theory": "Tkinter или aiogram",
            "practice": "Установка библиотеки",
            "tasks": "Установить выбранную библиотеку"
        },
        2: {
            "title": "День 3-4: Основы",
            "theory": "Создание окна/бота",
            "practice": "Мини-приложение",
            "tasks": "Создать базовый интерфейс"
        },
        3: {
            "title": "День 5-7: Проект",
            "theory": "Продвинутые функции",
            "practice": "Полноценное приложение",
            "tasks": "Завершить проект"
        }
    },
    "Неделя 7 — База данных": {
        1: {
            "title": "День 1: SQLite",
            "theory": "SQLite, подключение",
            "practice": "Создание БД",
            "tasks": "Создать базу данных"
        },
        2: {
            "title": "День 2: CRUD",
            "theory": "CREATE, READ, UPDATE, DELETE",
            "practice": "Операции с БД",
            "tasks": "Реализовать все операции"
        },
        3: {
            "title": "День 3-7: Проект",
            "theory": "Интеграция с проектом",
            "practice": "To-Do с БД",
            "tasks": "Перевести To-Do на SQLite"
        }
    },
    "Неделя 8 — Финальный проект": {
        1: {
            "title": "День 1-7: Финальный проект",
            "theory": "Выбери проект",
            "practice": "Telegram-бот/игра/парсер/расходы",
            "tasks": "Завершить полноценный проект"
        }
    }
}

MOTIVATIONAL_QUOTES = [
    "Каждый день приближает тебя к цели!",
    "Ты молодец, что учишься!",
    "Практика - ключ к успеху!",
    "Ошибки - это часть обучения!",
    "Продолжай, ты на правильном пути!",
    "Маленькие шаги ведут к большим результатам!",
    "Сегодня ты станешь лучше, чем вчера!",
    "Программирование - это творчество!",
    "Каждая строка кода - это прогресс!",
    "Верь в себя и у тебя всё получится!"
]

NOTIFICATION_MESSAGES = [
    "Время учить Python! Не забудь про практику!",
    "Привет! Как идёт обучение? Сейчас самое время для практики!",
    "Напоминаю: 1-1.5ч теории, 1-2ч практики!",
    "Python ждёт тебя! Открой приложение и учись!",
    "День не прошёл зря, если ты написал хоть строчку кода!",
    "Учишь Python? Отлично! Практикуйся каждый день!",
    "Программирование - это навык. Чем больше практики, тем лучше!",
    "Сделай перерыв и реши пару задач на Codewars!",
    "Помни: консистентность важнее интенсивности!",
    "Твой прогресс зависит от тебя! Начни прямо сейчас!"
]

def get_day_info(week, day):
    return DAYS_DATA.get(week, {}).get(day, {})

def can_proceed(data):
    key = f"{data['current_week']}_{data['current_day']}"
    return data.get('completed_days', {}).get(key, False)

def advance_day(data):
    weeks = list(DAYS_DATA.keys())
    current_idx = weeks.index(data['current_week'])
    week_data = DAYS_DATA[data['current_week']]
    max_day = max(week_data.keys())
    
    if data['current_day'] < max_day:
        data['current_day'] += 1
    else:
        if current_idx < len(weeks) - 1:
            current_idx += 1
            data['current_week'] = weeks[current_idx]
            data['current_day'] = 1
        else:
            return False
    return True

def get_motivation():
    import random
    return random.choice(MOTIVATIONAL_QUOTES)
