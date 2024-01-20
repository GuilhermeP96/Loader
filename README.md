# Loader Project
 
O projeto Loader é uma ferramenta de linha de comando para carregar dados de arquivos CSV em bancos de dados Oracle, otimizando o processo de importação e garantindo a correta formatação dos dados.
 
## Pré-requisitos
 
- Java 8: Necessário para a execução da aplicação.
 
### Para desenvolvedores
 
- ojdbc8.
 
## Configuração
 
### Variáveis de Ambiente
 
Configure as seguintes variáveis de ambiente antes de executar a aplicação Loader:
 
- `DB_USER_ENV`: Define o usuário do Oracle Database.
- `DB_PASSWORD_ENV`: Define a senha do usuário do Oracle Database.
 
As variáveis garantem segurança ao acesso às credenciais do banco de dados.
 
### Uso
 
Para executar a aplicação, configure o arquivo `configLoader.txt` com os detalhes da conexão e do arquivo CSV, e execute o comando:
 
```bash
java -jar Loader.jar <caminho para config.txt>
```
 
### Arquivos de Configuração
 
O arquivo `configLoader.txt` deve ser preenchido com detalhes como instância do banco, tabela de destino, caminho do arquivo CSV, separador de colunas, caractere delimitador, formato de data e decimal, e tamanho do lote.
 
## Documentação Adicional
 
A documentação do código-fonte está disponível nos comentários das classes e métodos. As convenções de codificação seguem práticas recomendadas de desenvolvimento Java.
 
## Desempenho
 
A ferramenta Loader demonstra eficiência na importação de dados, reduzindo significativamente o tempo necessário em comparação com métodos tradicionais.
 
## Contribuição
 
Para contribuir com o projeto Loader, entre em contato via e-mail.
 
## Licença
 
Este projeto é licenciado sob a MIT License - veja o arquivo LICENSE.md para detalhes.
 
## Download
 
[Loader Releases](https://github.com/GuilhermeP96/Loader/releases)
