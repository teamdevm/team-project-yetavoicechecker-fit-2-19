import librosa as lr
import numpy as np
import psycopg2
import sys
from sys import argv
from keras.layers import Dense, LSTM, Activation
from keras.models import Sequential
from keras.optimizers import Adam
file,adress=argv
DB_URI='postgres://maeispmbyimqkw:e3cb61bf41af040492b34009ea51bf3dca8b9a3744aa9d71e0930f7642028509@ec2-54-228-32-29.eu-west-1.compute.amazonaws.com:5432/d442i6fm8881j6'
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
def prepare_audio(aname):
  # Загружаем и подготавливаем данные
  print('loading %s' % aname)
  audio, _ = lr.load(aname, sr=SR)
  audio = filter_audio(audio) # Убираем тишину и пробелы между словами
  data = lr.stft(audio, n_fft=FFT).swapaxes(0, 1) # Извлекаем спектрограмму
  samples = []
  for i in range(0, len(data)):
    samples.append(np.abs(data[i])) # Создаем обучающую выборку
  srsamples=[0]*513 
  mx=0
  for sample in samples:
      for i in range(513):
          srsamples[i]+=sample[i]
  for i in range(513):
      srsamples[i]=srsamples[i]/len(samples)
      mx+=srsamples[i]
  mx=mx/513
  for i in range(513):
      srsamples[i]=srsamples[i]-mx
  return (srsamples)
def skhozh (x,y):
    chis,zn1,zn2=0,0,0
    for i in range(513):
        chis+=x[i]*y[i]
        zn1+=x[i]*x[i]
        zn2+=y[i]*y[i]
    a=np.abs(chis/((zn1**(0.5))*(zn2**(0.5))))
    return (a)

db_connection=psycopg2.connect(DB_URI,sslmode="require")
db_object=db_connection.cursor() 
x = prepare_audio(adress)
db_object.execute("SELECT * FROM  moshenniki WHERE times=2")
result=db_object.fetchall()
s=0
for results in result:
    fa=skhozh(x,results[0])
    if (fa>0.85):
        s=1
print(s)
