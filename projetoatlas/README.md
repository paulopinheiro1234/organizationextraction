# AtlasExtractorNovo

🧠 O que o programa faz
Lê as páginas indicadas do atlas_2022.pdf
Divide visualmente as páginas em dois blocos (esquerda/direita)
Aplica OCR para reconhecer o texto nas imagens
Extrai campos como:
Acrónimo
Nome do centro
Website
Guarda os resultados em ficheiros .csv ou imprime no terminal

Este projeto Java tem como objetivo **extrair automaticamente informações de centros de investigação a partir do ficheiro PDF `atlas_2022.pdf`**. O ficheiro PDF inclui dados como o nome do centro, acrónimo e website. 
O programa utiliza OCR com Tesseract e ferramentas de NLP para processar as páginas.
Requisitos

Antes de executar este projeto, certifica-te de que tens instalado:
- **Java JDK 11+**
- **Apache Maven**
- **Tesseract OCR**
- **atlas_2022.pdf** (colocar em `src/main/resources/`)


O projeto usa as seguintes bibliotecas (declaradas em `pom.xml`):
xml
<dependencies>
  <!-- Apache PDFBox para leitura de PDFs -->
  <dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <version>2.0.27</version>
  </dependency>

  <!-- Tesseract OCR wrapper -->
  <dependency>
    <groupId>net.sourceforge.tess4j</groupId>
    <artifactId>tess4j</artifactId>
    <version>5.4.0</version>
  </dependency>

  <!-- Biblioteca para manipulação de imagens (ImageIO) -->
  <dependency>
    <groupId>com.github.jai-imageio</groupId>
    <artifactId>jai-imageio-core</artifactId>
    <version>1.4.0</version>
  </dependency>
</dependencies>


Windows
Instala o Tesseract a partir do site: https://github.com/tesseract-ocr/tesseract
Durante a instalação, grava o caminho da pasta tesseract.exe
Adiciona essa pasta às variáveis de ambiente do sistema (Path)


Linux (Ubuntu/Debian)
sudo apt update
sudo apt install tesseract-ocr

macOS (via Homebrew)
brew install tesseract

Compilar o Projeto
mvn compile

Executar o Projeto
mvn exec:java

Se tiveres problemas com:
Tesseract não reconhecido → verifica o Path
Texto não detetado → certifica-te de que o tessdata está instalado
OCR lento → considera reduzir a resolução das imagens





