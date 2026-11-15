import html, subprocess, sys
OUT = sys.argv[1]
F = "DejaVu Sans"

def cls(name, attrs=(), meths=(), stereo=None, done=False):
    fill = "#E8F3EA" if done else "#FFFFFF"
    head = (f'<FONT POINT-SIZE="9">&laquo;{stereo}&raquo;</FONT><BR/>' if stereo else "") + f'<B>{html.escape(name)}</B>'
    rows = f'<TR><TD BGCOLOR="{fill}">{head}</TD></TR>'
    if attrs:
        rows += '<TR><TD ALIGN="LEFT" BALIGN="LEFT">' + "<BR/>".join(html.escape(a) for a in attrs) + '</TD></TR>'
    if meths:
        rows += '<TR><TD ALIGN="LEFT" BALIGN="LEFT">' + "<BR/>".join(html.escape(m) for m in meths) + '</TD></TR>'
    return f'<<TABLE BORDER="0" CELLBORDER="1" CELLSPACING="0" CELLPADDING="4">{rows}</TABLE>>'

C = {}
def add(pkg, key, *a, **k): C.setdefault(pkg, []).append((key, cls(*a, **k)))

# core
add("core","Block","Block",["- index: int","- timestamp: long","- previousHash: String","- transactions: List<String>","- merkleRoot: String","- validator: String","- currentHash: String","- signature: String","- votes: List<Vote>"],["+ calculateHash(): String","+ isValid(): boolean","+ getMerkleTree(): MerkleTree","+ setSignature(s)","+ addVote(v: Vote)"],done=True)
add("core","BlockChain","BlockChain",["- chain: ArrayList<Block>"],["+ add(b: Block): boolean","+ isValid(): boolean","+ getInvalidIndex(): int","+ getLastBlockHash(): String","+ save(f) / load(f)"],done=True)
add("core","MerkleTree","MerkleTree",["- hashTree: List<List<String>>","- elements: List<String>"],["+ getRoot(): String","+ getProof(data): List<String>","+ isProofValid(data, proof): boolean","+ isValid(): boolean"],done=True)
add("core","Vote","Vote",["- validator: String","- signature: String"],[],done=True)
# utils
add("utils","SecurityUtils","SecurityUtils",[],["+ getHash(data): String","+ generateKeyPair(): KeyPair","+ sign(data, priv): String","+ verify(data, sig, pub): boolean","+ keyToBase64(key) / publicKeyFromBase64(txt)"],done=True)
add("utils","Conversion","Conversion",[],["+ objectToBase64(obj): String","+ base64ToObject(txt): T"],done=True)
# identity
add("identity","Role","Role",["DGAV, LAB, DOP,","APICULTOR, LOJA"],[],stereo="enum")
add("identity","Identity","Identity",["- name: String","- role: Role","- keyPair: KeyPair","- did: String"],["+ getDid(): String","+ getPublicKeyBase64(): String","+ sign(data): String"])
add("identity","CredentialType","CredentialType",["REGISTO_APICULTOR, REGISTO_APIARIO,","REGISTO_LOJA, LOTE,","ANALISE_LOTE, CERTIFICADO_ORIGEM"],[],stereo="enum")
add("identity","Credential","Credential",["- id: String","- type: CredentialType","- issuer: String (DID)","- subject: String","- data: Map<String,String>","- issuedAt: long","- signature: String"],["+ getHash(): String","+ sign(issuer: Identity)","+ isSignatureValid(pub): boolean"])
add("identity","OffChainStore","OffChainStore",["- credentials: Map<String,Credential>"],["+ put(c) / get(id)","+ save(f) / load(f)"])
# transactions
add("tx","Transaction","Transaction",["- author: String (DID)","- timestamp: long","- signature: String"],["+ getContent(): String","+ sign(id: Identity)","+ isSignatureValid(pub): boolean","+ toBase64(): String"],stereo="abstract")
add("tx","GenesisTx","GenesisTx",["- authorities: List<Authority>"])
add("tx","IssueCredentialTx","IssueCredentialTx",["- credentialId, credentialType","- subject, credentialHash","- subjectKey (opcional)"])
add("tx","RevokeTx","RevokeTx",["- credentialId","- reason"])
add("tx","PackTx","PackTx",["- lot","- firstJar, lastJar","- weightKg"])
add("tx","SaleTx","SaleTx",["- jarId","- shop"])
add("tx","FraudAlertTx","FraudAlertTx",["- jarId","- reason"])
# ledger
add("ledger","LedgerState","LedgerState",["- chain: BlockChain"],["+ publicKeyOf(did): PublicKey","+ roleOf(did): Role","+ isRevoked(credId): boolean","+ lotBalance(lot): Balance","+ isSold(jarId): boolean"])
add("ledger","Rules","Rules",["TOLERANCE = 0.02"],["+ check(tx, state): Transaction","   (rejeita ou converte em FraudAlertTx)"])
# consensus
add("consensus","PeerNode","PeerNode",["- id: String","- authority: Identity (ou null)","- chain: BlockChain","- online: boolean"],["+ validate(b): Vote","+ commit(b): boolean","+ syncFrom(other: Node)"])
add("consensus","ProofOfAuthority","ProofOfAuthority",["- validators: List<Node>"],["+ selectProposer(height): Node","+ propose(txs): Block","+ quorum(): int   (n/2 + 1)"])
add("consensus","LocalNetwork","LocalNetwork",["- nodes: List<Node>","- log: List<String>"],["+ broadcast(b: Block)","+ setOnline(node, on)"])
# service
add("service","ApisChainService","ApisChainService",["- network, poa, offChain"],["+ registerBeekeeper(...) / registerApiary(...)","+ createLot(...) / issueAnalysis(...)","+ issueCertificate(...) / revoke(...)","+ packJars(...) / sellJar(...)","+ verifyJar(id): VerificationResult"],stereo="fachada")
add("service","VerificationResult","VerificationResult",["- level: OK | AVISO | FALHA","- title: String","- checks: List<String>"])
# gui
add("gui","MainFrame","MainFrame (JFrame)",["- tabs: JTabbedPane"],[],stereo="Swing")
add("gui","Panels","Painéis (JPanel)",["ConsumerPanel, BeekeeperPanel,","LabPanel, DopPanel, ShopPanel,","DgavPanel, ChainPanel, NetworkPanel,","MerkleTreePanel (das aulas)"],[],stereo="Swing")

titles = {"core":"apischain.core","utils":"apischain.utils","identity":"apischain.identity","tx":"apischain.transactions","ledger":"apischain.ledger","consensus":"apischain.consensus","service":"apischain.service","gui":"apischain.gui"}
dot = [f'digraph G {{ rankdir=TB; newrank=true; compound=true; nodesep=0.35; ranksep=0.55; bgcolor="white";',
       f'graph [fontname="{F}", fontsize=12, labeljust=l]; node [shape=plaintext, fontname="{F}", fontsize=10]; edge [fontname="{F}", fontsize=9, color="#555555"];',
       f'labelloc=t; label=<<B>ApisChain · Diagrama de classes v1</B><BR/><FONT POINT-SIZE="10">Verde: implementado (11 nov) · Branco: previsto para a PoC</FONT>>; fontsize=16;']
for p, items in C.items():
    dot.append(f'subgraph cluster_{p} {{ label="{titles[p]}"; style="rounded"; color="#999999";')
    for k, lab in items: dot.append(f'{k} [label={lab}];')
    dot.append('}')
E = [
 ("BlockChain","Block",'arrowtail=diamond, dir=back, headlabel="*"'),
 ("Block","Vote",'arrowtail=diamond, dir=back, headlabel="*"'),
 ("Block","MerkleTree",'style=dashed, label="usa"'),
 ("Block","SecurityUtils",'style=dashed'),
 ("GenesisTx","Transaction",'arrowhead=empty'),("IssueCredentialTx","Transaction",'arrowhead=empty'),("RevokeTx","Transaction",'arrowhead=empty'),
 ("PackTx","Transaction",'arrowhead=empty'),("SaleTx","Transaction",'arrowhead=empty'),("FraudAlertTx","Transaction",'arrowhead=empty'),
 ("Transaction","Conversion",'style=dashed, label="Base64 no bloco"'),
 ("Identity","Role",''),("Credential","CredentialType",''),
 ("OffChainStore","Credential",'arrowtail=diamond, dir=back, headlabel="*"'),
 ("IssueCredentialTx","Credential",'style=dashed, label="hash"'),
 ("LedgerState","BlockChain",'label="lê"'),
 ("Rules","LedgerState",'style=dashed'),
 ("PeerNode","BlockChain",'arrowtail=diamond, dir=back, label="cópia própria"'),
 ("PeerNode","Identity",'label="autoridade"'),
 ("ProofOfAuthority","PeerNode",'label="validadores"'),
 ("ProofOfAuthority","Rules",'style=dashed, label="valida tx"'),
 ("LocalNetwork","PeerNode",'arrowtail=diamond, dir=back, headlabel="*"'),
 ("ApisChainService","ProofOfAuthority",''),("ApisChainService","LocalNetwork",''),("ApisChainService","OffChainStore",''),
 ("ApisChainService","VerificationResult",'style=dashed'),
 ("MainFrame","Panels",'arrowtail=diamond, dir=back'),("Panels","ApisChainService",'label="chama"'),
]
for a,b,attr in E: dot.append(f'{a} -> {b} [{attr}];')
dot.append('}')
open(OUT+".dot","w").write("\n".join(dot))
for fmt in ("png","svg"):
    subprocess.run(["dot",f"-T{fmt}","-Gdpi=130" if fmt=="png" else "-Gdpi=72",OUT+".dot","-o",f"{OUT}.{fmt}"],check=True)
