"""
None
"""
import toga
from toga.style import Pack
from toga.style.pack import COLUMN, LEFT, CENTER,RIGHT, ROW, Pack
import pathlib
from pathlib import Path





class VoiceReognize(toga.App):

    def startup(self):
        """
        Construct and show the Toga application.

        Usually, you would add your application to a main content box.
        We then create a main window (with a name matching the app), and
        show the main window.
        """
        
        ### Contacts_Screen
        self.view_box2 = toga.Box(style=Pack(direction=COLUMN,alignment=CENTER, background_color ='gray'))
        back_button = toga.Button("<<Главное меню", on_press = self.main_view, style = Pack(alignment = CENTER, font_size=19, background_color = 'red'))
        self.view_box2.style = Pack(direction=COLUMN, height=20, width=480)
        #IMGpath = Path(pathlib.Path.cwd(), 'src', 'voicereognize', 'resources', 'Call_Class.png')
        #self.cotnacts_class = toga.Image(path = IMGpath)
        #self.view = toga.ImageView(id='view1', image=self.cotnacts_class)
        #self.view_box2.add(self.view)
        self.view_box2.add(back_button)
        
        
        
        
        
        ###
        self.main_box = toga.Box(style=Pack(direction=COLUMN,alignment=CENTER, background_color ='gray', flex = 3))
        self.view_box = toga.Box()
        ###
        
        
        
        # Main Screen
        main_text = toga.Label("Идентификация голоса",style = Pack(text_align=CENTER, color = 'blue', font_size = 19, width=480))
        first_view = toga.Button('Последние входящие вызовы', on_press=self.first_view, style=Pack(padding=20, font_size = 19, background_color = "blue"))
        contacts_container = toga.Selection(items=['Все номера', 'Только из списка контактов', 'Отключить проверку'], style=Pack(text_align = CENTER, alignment = CENTER, font_size =19, width=480))
        #self.view_box.style = Pack(direction=ROW, padding=2)
        self.view_box.add(first_view)
        ###
        self.contacts_selection_box = toga.Box()
        self.contacts_selection_box.add(contacts_container)
        self.Text_box = toga.Box()
        self.Text_box.add(main_text)
        self.main_box.add(self.Text_box)
        self.main_box.add(self.view_box)
        self.main_box.add(self.contacts_selection_box)
        self.style=Pack(background_color="gray")
        self.main_window = toga.MainWindow(size=(640, 480),toolbar=None, resizeable=False, minimizable=False, factory=None, on_close=None)#title=self.formal_name)
        self.main_window.content = self.main_box
        self.main_window.show()
        
        
    def first_view(self, sender):
        self.main_box.remove(self.view_box)
        self.main_box.remove(self.contacts_selection_box)
        self.main_box.add(self.view_box2)
        self.main_window.show()
        
    def main_view(self, sender):
        self.main_box.remove(self.view_box2)
        self.main_box.add(self.view_box)
        self.main_box.add(self.contacts_selection_box)
        self.main_window.show()
         
        
    def second_view(self, widget):
        self.main_box.remove(self.view_box)
        self.view_box = toga.Box()
        self.view_box.style = Pack(direction=ROW, padding=2)
        screen_text = toga.Label('This screen will allow you to see your Second View')
        self.view_box.add(screen_text)
        self.main_box.add(self.view_box)
        self.main_window.show()
        
        


def main():
    return VoiceReognize()
