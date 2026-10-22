# 🚽 Fila do Banheiro (Restroom line manager)

Aplicativo Android para **organizar a fila do banheiro em escolas** e lugares parecidos. O professor cadastra suas turmas, controla quem está na fila e quem saiu da sala, recebe um alerta quando alguém demora mais de 10 minutos e acompanha, em gráficos, os dias de maior movimento, sem nunca expor dados individuais.

## 👥 Integrantes do grupo

**Turma:** 2B

**Integrantes:**

- Marcone Bezerra
- Marcelo José
- Marlon Saimon
- Carlos Henrique
- João Guilherme
- Thiago Guerra

---

## 📖 Como o app funciona

1. **Cadastro e login:** o usuário cria uma conta com e-mail e senha (mínimo de 6 caracteres) e entra no app.
2. **Turmas:** na tela inicial ele cria quantas turmas quiser (por exemplo, 1A, 2B). Cada turma tem seus próprios alunos, sua fila e suas estatísticas.
3. **Alunos:** os nomes entram de duas formas: importando um **PDF** (o app extrai os nomes e mostra uma tela de revisão antes de salvar) ou digitando manualmente em **Adicionar nomes**.
4. **Fila:** cada aluno está sempre em um de três estados:
   - 😊 **Disponível:** está na sala e pode entrar na fila;
   - ⏳ **Na fila:** aguardando, com a posição mostrada;
   - 🚶 **Fora agora:** saiu para o banheiro, com um cronômetro em andamento.
5. **Controle de saída:** o professor toca em **Liberar** para o primeiro da fila sair e em **Voltou** quando ele retorna. Um **limite de alunos fora ao mesmo tempo** (escolhido pelo usuário, por turma) bloqueia novas saídas até alguém voltar.
6. **Alerta de 10 minutos:** o cronômetro de quem está fora é um anel que se enche em 10 minutos. Passando disso, o cartão pisca em vermelho e o app envia uma **notificação**.
7. **Estatísticas:** uma aba mostra quantas vezes a turma foi ao banheiro em cada dia da semana e destaca o dia mais movimentado.
8. **Nuvem:** o usuário pode fazer **backup** e **restaurar** seus dados pela conta. Ao sair da conta, o app faz o backup automaticamente antes de limpar o aparelho.

---

## ✨ Funcionalidades

| Área | O que o app faz |
|---|---|
| **Conta** | Cadastro e login com e-mail e senha; sair da conta com backup automático |
| **Turmas** | Criar e excluir várias turmas; cada uma com configurações próprias |
| **Alunos** | Importar nomes de PDF, adicionar manualmente, renomear e excluir |
| **Fila** | Entrar na fila, sair da fila, liberar, registrar retorno, posição na fila |
| **Limite** | Máximo de alunos fora ao mesmo tempo, de 1 a 20 (padrão: 1), definido por turma |
| **Alerta** | Cronômetro com anel de progresso, aviso visual em vermelho e notificação após 10 minutos |
| **Busca** | Barra de pesquisa com **sugestões** em tempo real (ignora acentos e maiúsculas) |
| **Estatísticas** | Gráfico de idas ao banheiro por dia da semana, por turma, com o dia mais movimentado |
| **Backup** | Backup e restauração na nuvem (Firebase) |
| **Idiomas** | Português (Brasil), que é o padrão, e inglês, com troca instantânea pelo botão 🌐 |
| **Visual** | Tema escuro com gradientes, avatares coloridos e animações |

---

## 🎯 Público-alvo

- **Professores e monitores** que precisam organizar as saídas da sala sem interromper a aula;
- **Coordenação e direção escolar**, que podem usar as estatísticas para entender os horários e dias de maior movimento e planejar melhor;
- **Escolas de ensino fundamental e médio**, cursos e outros lugares com banheiro compartilhado e controle de saída.

---

## 🛠️ Tecnologias utilizadas

| Tecnologia | Para que serve no projeto |
|---|---|
| **Kotlin** | Linguagem principal do app |
| **Android Studio** | Ambiente de desenvolvimento |
| **Jetpack Compose + Material 3** | Interface do usuário (telas, animações e tema) |
| **Room (SQLite)** | Banco de dados local, com as tabelas `ClassRoom`, `Student` e `Trip` |
| **Kotlin Coroutines e Flow** | Operações assíncronas e atualização automática das telas |
| **ViewModel (Lifecycle)** | Estado e regras do app separados da interface |
| **WorkManager** | Notificação de alerta após 10 minutos, mesmo com o app fechado |
| **PdfBox-Android** | Leitura de texto dos PDFs para extrair os nomes dos alunos |
| **Firebase Authentication** | Cadastro e login por e-mail e senha |
| **Cloud Firestore** | Backup e restauração dos dados na nuvem |
| **KSP** | Geração de código do Room em tempo de compilação |
| **Gradle (Kotlin DSL)** | Gerenciamento de dependências e build |

**Requisito do aparelho:** Android 8.0 (API 26) ou superior.

### 🎨 Paleta de cores

| Cor | Código | Uso |
|---|---|---|
| Azul-marinho | `#0C2840` | Fundo do app |
| Azul | `#1A90D9` | Cor principal e botões |
| Cinza-azulado | `#2E3940` | Cartões e superfícies |
| Ciano | `#32BAD9` | Destaques e títulos de seção |
| Verde-água | `#6AD9C4` | Ações principais e dia mais movimentado |

---

## 🧱 Arquitetura e estrutura do código

O app segue o padrão **MVVM** e é **local-first**: o banco Room no aparelho é a fonte principal dos dados, e o Firestore guarda uma cópia de backup.

```
app/src/main/java/com/example/restroomqueue/
├── MainActivity.kt   # ponto de entrada e permissão de notificações
├── UI.kt             # todas as telas e componentes (Compose)
├── VM.kt             # ViewModel: regras da fila, login, backup e idioma
├── Data.kt           # entidades, DAO e banco de dados (Room)
├── Cloud.kt          # backup e restauração no Firestore
├── PdfNames.kt       # extração de nomes a partir de PDF
├── Overdue.kt        # notificação de 10 minutos (WorkManager)
├── Lang.kt           # troca de idioma (PT-BR / EN)
└── Theme.kt          # cores e tema
```

---

## 🔒 Privacidade

- **As estatísticas são anônimas:** cada ida ao banheiro é guardada apenas com a **turma** e o **horário**, sem ligação com nenhum aluno. Por isso é impossível saber quantas vezes um aluno específico foi.
- **Dados na nuvem:** o backup envia os nomes dos alunos ao Firestore. Os nomes são dados pessoais, muitas vezes de menores de idade, então a escola deve autorizar o uso, em conformidade com a **LGPD**.
- **Regras do Firestore:** cada conta só pode ler e gravar os próprios dados (veja as regras em `SETUP.md`).
- O estado da fila (quem está esperando ou fora) **não** é enviado ao backup.

---

## ▶️ Como executar

1. Abra o projeto no **Android Studio**.
2. Crie um projeto no **Firebase**, adicione o app Android com o pacote `com.example.restroomqueue` e coloque o arquivo `google-services.json` dentro da pasta `app/`.
3. No Firebase, ative **Authentication → E-mail/senha** e crie o banco **Firestore** com as regras do `SETUP.md`.
4. Faça o **Sync** do Gradle e execute em um emulador ou em um celular com a depuração USB ativada.

---

## 🚧 Limitações e próximos passos

- A leitura de PDF funciona apenas com PDFs que contêm texto; PDFs escaneados (imagens) precisariam de OCR.
- O backup é de **um aparelho por conta** (vale o último backup feito), não uma sincronização em tempo real.
- Ideias futuras: recuperação de senha, exportar as estatísticas em PDF e sincronização em tempo real entre aparelhos.
