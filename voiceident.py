import librosa as lr
import numpy as np

from keras.layers import Dense, LSTM, Activation
from keras.models import Sequential
from keras.optimizers import Adam
# туть создание нейросетки, она рабочая, но я пока не очень понимаю, как её использовать
SR = 16000 # Частота дискретизации
LENGTH = 16 # Количество блоков за один проход нейронной сети
OVERLAP = 8 # Шаг в количестве блоков между обучающими семплами
FFT = 1024 # Длина блока (64 мс)
def filter_audio(audio):
  # Считаем энергию голоса для каждого блока в 125 мс
  apower = lr.amplitude_to_db(np.abs(lr.stft(audio, n_fft=2048)), ref=np.max)

  # Суммируем энергию по каждой частоте, нормализуем
  apsums = np.sum(apower, axis=0)**2
  apsums -= np.min(apsums)
  apsums /= np.max(apsums)

  # Сглаживаем график, чтобы сохранить короткие пропуски и паузы, убрать резкость
  apsums = np.convolve(apsums, np.ones((9,)), 'same')
  # Нормализуем снова
  apsums -= np.min(apsums)
  apsums /= np.max(apsums)

  # Устанавливаем порог в 35% шума над голосом
  apsums = np.array(apsums > 0.35, dtype=bool)

  # Удлиняем блоки каждый по 125 мс
  # до отдельных семплов (2048 в блоке)
  apsums = np.repeat(apsums, np.ceil(len(audio) / len(apsums)))[:len(audio)]

  return audio[apsums] # Фильтруем!
def prepare_audio(aname, target=False):
  # Загружаем и подготавливаем данные
  print('loading %s' % aname)
  audio, _ = lr.load(aname, sr=SR)
  audio = filter_audio(audio) # Убираем тишину и пробелы между словами
  data = lr.stft(audio, n_fft=FFT).swapaxes(0, 1) # Извлекаем спектрограмму
  samples = []

  for i in range(0, len(data) - LENGTH, OVERLAP):
    samples.append(np.abs(data[i:i + LENGTH])) # Создаем обучающую выборку

  results_shape = (len(samples), 1)
  results = np.ones(results_shape) if target else np.zeros(results_shape)
  return np.array(samples), results

## Список всех записей, тут будут файлы из локального хранилища
voices = [("woman2.wav", True),
          ("woman2.1.wav", True),
          ("woman2.2.wav", True),
          ("woman1.wav", False),
          ("woman1.1.wav", False),
          ("woman1.2.wav", False),
          ("man1.1.wav", False),
          ("man1.2.wav", False),
          ("man1.wav", False)]

## Объединяем обучающие выборки
X, Y = prepare_audio(*voices[0])
for voice in voices[1:]:
  dx, dy = prepare_audio(*voice)
  X = np.concatenate((X, dx), axis=0)
  Y = np.concatenate((Y, dy), axis=0)
  del dx, dy

## Случайным образом перемешиваем все блоки
perm = np.random.permutation(len(X))
X = X[perm]
Y = Y[perm]

## Создаем модель
model = Sequential()
model.add(LSTM(128, return_sequences=True, input_shape=X.shape[1:]))
model.add(LSTM(64))
model.add(Dense(64))
model.add(Activation('tanh'))
model.add(Dense(16))
model.add(Activation('sigmoid'))
model.add(Dense(1))
model.add(Activation('hard_sigmoid'))

## Компилируем и обучаем модель
model.compile(Adam(lr=0.005), loss='binary_crossentropy', metrics=['accuracy'])
model.fit(X, Y, epochs=15, batch_size=32, validation_split=0.2)

## Тестируем полученную в итоге модель
print(model.evaluate(X, Y))
## Сохраняем модель для дальнейшего использования
model.save('model.hdf5')
