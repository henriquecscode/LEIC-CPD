Trabalho Prático

### Descrição

A ideia é usar primeiro C e fazer assim e depois escolher outra linguagem e verificar se os problemas que existem em C existem também nessa linguagem ou não. Por outro lado, tentar perceber o porquê deste impacto, para o qual utlizaremos Performance API (PAPI).

PAPI corre em Linux. Só precisamos do compilador de C, supostamente. Para a outra linguagem, podemos usar o que acharmos melhor, por exemplo em Windows. Procura-se ver os contadores mais adequados.

Primeira versão é como está, segunda é trocar as duas linhas no ciclo, a terceira é o algoritmo em blocos.

### Notas

Na primeira versão, estamos sempre a acumular tudo no mesmo elemento, acedendo sempre às mesmas colunas repetidamente. Segue a forma como é feita na álgebra. Depois de todos os *k* para um igual *i* e *j*, sobe o *j* em 1 e só depois o *i.* 

Com $i = 0$, $j = 0$ e $k = n - 1$:

$$
C(0, 0) = A(0, n - 1)*B(n-1, 0)
$$

$$
C(i, j) \text { } += A(i, k) * B(k, j)
$$

Percorre-se, portanto, a matriz ao longo da coluna. $O(n) = 2n^3$. Vai precisar-se do triplo do espaço para uma só matriz: primeira matriz, segunda matriz e a matriz resultado.

Na segunda versão, trocamos as linhas do ciclo de *k* e *j*:

$$
C(i, j) \text { } += A(i, k) * B(k, j)
$$

Na primeira iteração, é igual à segunda versão, mas depois, quando se altera *j* para 1, por exemplo:

$$
C(0, 1) \text { }+= A(0, 0)*B(0, 1)
$$

Esta multiplicação é feita linha a linha, aumentando em colunas e não em linhas - primeiro é (0, 0) com (0, 0), depois é (0, 0) e (0, 1) - mesma linha, coluna seguinte na segunda matriz. Ao longo do tempo é que os valores ficam completos.

Percorre-se, portanto, a matriz ao longo das suas linhas.

$$
C(0, n - 1) \text { } A(0, 0) * B(0,n - 1)
$$

Em memória os dados não estão organizados na mesma sequência em que são utilizados ou estão representados enquanto matriz - isto é importante. O sistema funciona sempre da mesma forma e vai ler os dados em segmentos. Quando é pedido um valor, um segmento é colocado em cache.

$$
t = \frac {2n^3} {Capacidade} 
$$

<aside>
❗ vTUNE - é gratuito, it seems? E dá em Windows.
</aside>

### Comandos PAPI

```jsx
papi_avail - a - ver os contadores disponíveis.
```

### Multiplicação de Matrizes em Bloco

A matriz P de dimensões 4x4, pode ser dividida em blocos de 2x2, dividindo a operação de produto em operações mais pequenas que podem ser paralelizadas.

O resultado é na mesma $2n^3$, não se altera o número de operações. É uma questão de paralelismo. Valores que funcoinam melhor atualmente: 128, 256, 512.  Elementos são sub matrizes.



### Resultados PAPI

#### Alínea 1 C++

| n    | t (s)   | FLOPS         |
| ---- | ------- | ------------- |
| 600  | 0.369   | 1170731707.32 |
| 1000 | 1.846   | 1083423618.63 |
| 1400 | 6.692   | 820083682.01  |
| 1800 | 29.539  | 394867801.89  |
| 2200 | 64.955  | 327857747.67  |
| 2600 | 123.104 | 285547179.62  |
| 3000 | 208.961 | 258421427.92  |

#### Alínea 2 C++

| n     | t (s)   | FLOPS         |
| ----- | ------- | ------------- |
| 600   | 0.138   | 3130434782.61 |
| 1000  | 0.748   | 2673796791.44 |
| 1400  | 1.809   | 3033720287.45 |
| 1800  | 4.162   | 2802498798.65 |
| 2200  | 9.325   | 2283753351.21 |
| 2600  | 12.304  | 2856957087.13 |
| 3000  | 19.486  | 2771220363.34 |
| 4096  | 58.017  | 2368942783.53 |
| 6144  | 172.643 | 2686795688.03 |
| 8192  | 410.192 | 2680480428.13 |
| 10240 | 799.755 | 2685176895.42 |

#### Alínea 3 C++

| n     | nº blocos | t (s)    | FLOPS         |
| ----- | --------- | -------- | ------------- |
| 4096  | 128       | 119.196  | 1153050047.59 |
| 4096  | 256       | 238.129  | 577161763.04  |
| 4096  | 512       | 867.998  | 158340172.99  |
| 6144  | 128       | 361.453  | 1283310604.61 |
| 6144  | 256       | 363.981  | 1274397476.70 |
| 6144  | 512       | 1135.45  | 408522143.62  |
| 8192  | 128       | 228.474  | 4812414663.27 |
| 8192  | 256       | 7526.868 | 146078239.68  |
| 8192  | 512       | 7355.487 | 149481825.99  |
| 10240 | 128       | 1661.99  | 1292115865.92 |
| 10240 | 256       | 1713.75  | 1253090385.41 |
| 10240 | 512       | 5839.257 | 367766592.22  |

### Resultados Tempos

#### Alínea 1 Java

| n    | t (s)       | FLOPS        |
| ---- | ----------- | ------------ |
| 600  | 0.6985596   | 618415379.30 |
| 1000 | 4.4721911   | 447208081.07 |
| 1400 | 21.0427352  | 260802597.56 |
| 1800 | 55.3692335  | 210658505.86 |
| 2200 | 105.8207027 | 201246064.87 |
| 2600 | 162.0726782 | 216890350.62 |
| 3000 | 258.0806974 | 209236880.34 |

#### **Alínea 2** Java

| **n** | t (s)      | FLOPS         |
| ----- | ---------- | ------------- |
| 600   | 0.1966419  | 2196886828.29 |
| 1000  | 0.979317   | 2042239642.53 |
| 1400  | 2.5388417  | 2161615669.07 |
| 1800  | 5.21805    | 2235317791.13 |
| 2200  | 9.5404427  | 2232181531.79 |
| 2600  | 15.7125223 | 2237196506.64 |
| 3000  | 26.5210152 | 2036121151.20 |

