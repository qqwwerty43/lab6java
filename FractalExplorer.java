package com.company;

import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Double;
import java.awt.event.*;
import javax.swing.JFileChooser.*;
import javax.swing.filechooser.*;
import java.awt.image.*;
import java.awt.*;
import javax.swing.*;

public class FractalExplorer {

    /** Элементы, которые надо блокировать методом enableUI **/
    private JComboBox comboBox;
    private JButton button_reset;
    private JButton button_save;

    /** Размер экрана **/
    private int size;

    /** Ссылка на изображение **/
    private JImageDisplay image;

    /** Ссылка на объект "Фрактал" **/
    private FractalGenerator generator;

    /** Отображаемый диапазон в комплексной области **/
    private Rectangle2D.Double range;

    /** Количество оставшихся строк, в которых надо посчитать цвет точек **/
    private int rowsRemaining;

    /**
     * Конструктор класса
     **/
    public FractalExplorer(int new_size) {
        this.size = new_size;
        range = new Rectangle2D.Double();
        generator = new Mandelbrot();
        generator.getInitialRange(range);
        image = new JImageDisplay(size, size);
    }

    /**
     * Метод инициализирует графический интерфейс пользователя
     **/
    public void createAndShowGUI() {
        /**
         * Инициализация окна
         **/
        JFrame frame = new JFrame("Fractal");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(size, size);
        frame.setLayout(new BorderLayout());

        /**
         * Инициализация элементов
         **/
        comboBox = new JComboBox();
        button_reset = new JButton("Reset");
        button_save = new JButton("Save");
        JPanel bottom_panel = new JPanel();
        JPanel top_panel = new JPanel();
        JLabel lable = new JLabel("Fractal: ");
        FractalGenerator fractal_mandelbrot = new Mandelbrot();
        FractalGenerator fractal_tricorn = new Tricorn();
        FractalGenerator fractal_burningship = new BurningShip();

        /**
         * Добавление элементов на окно
         **/
        bottom_panel.add(button_save);
        bottom_panel.add(button_reset);
        top_panel.add(lable);
        top_panel.add(comboBox);
        frame.add(bottom_panel, BorderLayout.SOUTH);
        frame.add(image, BorderLayout.CENTER);
        comboBox.addItem(fractal_mandelbrot);
        comboBox.addItem(fractal_tricorn);
        comboBox.addItem(fractal_burningship);
        frame.add(top_panel, BorderLayout.NORTH);
        frame.pack();
        frame.setVisible(true);
        frame.setResizable(false);

        /**
         * Действие кнопки reset
         **/
        ActionListener DisplayReset = new ResetDisplay();
        button_reset.addActionListener(DisplayReset);

        /**
         * Масштабирование по нажатию мыши
         **/
        MouseListener zoom = new ZoomFractal();
        image.addMouseListener(zoom);

        /**
         * Выбор фрактала
         **/
        ActionListener choose = new ChooseFractal();
        comboBox.addActionListener(choose);

        /**
         * Сохранение изображения
         **/
        ActionListener save = new SaveImage();
        button_save.addActionListener(save);
    }

    /**
     * Метод рисует фрактал
     **/
    public void drawFractal() {
        enableUI(false);
        rowsRemaining = size;
        for (int x = 0; x < size; x++){
            FractalWorker drawRow = new FractalWorker(x);
            drawRow.execute();
        }
    }

    /**
     * Метод включает и выключает кнопки и комбо-бокс
     **/
    private void enableUI(boolean value) {
        comboBox.setEnabled(value);
        button_reset.setEnabled(value);
        button_save.setEnabled(value);
    }

    /**
     * Внутренний класс, перерисовывающий фрактал
     **/
    private class ResetDisplay implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            generator.getInitialRange(range);
            FractalExplorer.this.drawFractal();
        }
    }

    /**
     * Внутренний класс, мастабирующий фрактал
     **/
    private class ZoomFractal extends MouseAdapter implements MouseListener {

        @Override
        public void mousePressed(MouseEvent e) {
            if (rowsRemaining != 0) {
                return;
            }
            int x = e.getX();
            int y = e.getY();
            double xCoord = generator.getCoord(range.x, range.x +range.width, size, x);
            double yCoord = generator.getCoord(range.y, range.y +range.height, size, y);
            generator.recenterAndZoomRange(range,xCoord, yCoord, 0.5);
            FractalExplorer.this.drawFractal();
        }
    }

    /**
     * Внутренний класс, переключающий на выбранный фрактал
     **/
    private class ChooseFractal implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            JComboBox target = (JComboBox) e.getSource();
            generator = (FractalGenerator) target.getSelectedItem();
            generator.getInitialRange(range);
            FractalExplorer.this.drawFractal();
        }
    }

    /**
     * Внутренний класс, сохраняющий изображение
     **/
    private class SaveImage implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            JFileChooser chooser = new JFileChooser();
            FileFilter filter = new FileNameExtensionFilter("PNG Images", "png");
            chooser.setFileFilter(filter);
            chooser.setAcceptAllFileFilterUsed(false);
            int userSelection = chooser.showSaveDialog(image);
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                java.io.File file = chooser.getSelectedFile();
                String file_name = file.toString();
                try {
                    BufferedImage displayImage = image.getImage();
                    javax.imageio.ImageIO.write(displayImage, "png", file);
                }
                catch (Exception exception) {
                    JOptionPane.showMessageDialog(image, exception.getMessage(), "Cannot Save Image", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    /**
     * Внутренний класс, отвечающий за многопоточную прорисовку фрактала
     **/
    private class FractalWorker extends SwingWorker<Object, Object> {

        /** y-координата, строки, которую будет вычислять данный поток **/
        int yCoordinate;

        /** Массив для хранения цветов точек данной строки **/
        int[] computed_RGB;

        /**
         * Конструктор внутреннего класса
         **/
        private FractalWorker(int target_row) {
            yCoordinate = target_row;
        }

        /**
         * Метод вычисляет значения цветов точек для данной строки
         **/
        protected Object doInBackground() {

            computed_RGB = new int[size];
            double xCoord;
            double yCoord;
            int iterations_number;
            for (int i = 0; i < computed_RGB.length; i++) {
                xCoord = generator.getCoord(range.x, range.x + range.width, size, i);
                yCoord = generator.getCoord(range.y, range.y + range.height, size, yCoordinate);
                iterations_number = generator.numIterations(xCoord, yCoord);
                if (iterations_number == -1){
                    computed_RGB[i] = 0;
                }
                else {
                    float hue = 0.7f + (float) iterations_number / 200f;
                    int rgbColor = Color.HSBtoRGB(hue, 1f, 1f);
                    computed_RGB[i] = rgbColor;
                }
            }
            return null;
        }

        /**
         * Метод, рисующий данную строку
         **/
        protected void done() {
            for (int i = 0; i < computed_RGB.length; i++) {
                image.drawPixel(i, yCoordinate, computed_RGB[i]);
            }
            image.repaint(0, 0, yCoordinate, size, 1);
            rowsRemaining--;
            if (rowsRemaining == 0) {
                enableUI(true);
            }
        }
    }

    /**
     * Точка входа в приложение
     **/
    public static void main(String[] args) {
        int ScreenSize = 800;
        FractalExplorer fractal = new FractalExplorer(ScreenSize);
        fractal.createAndShowGUI();
        fractal.drawFractal();
    }

}